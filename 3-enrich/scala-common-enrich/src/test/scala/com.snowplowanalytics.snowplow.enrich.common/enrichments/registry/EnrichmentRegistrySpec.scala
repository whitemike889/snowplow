/*
 * Copyright (c) 2014-2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics
package snowplow
package enrich
package common
package enrichments
package registry

// Java
import java.net.URI
import java.lang.{Byte => JByte}

import com.snowplowanalytics.iglu.client.repositories.RepositoryRefConfig
import com.snowplowanalytics.snowplow.enrich.common.utils.TestResourcesRepositoryRef

//import com.snowplowanalytics.iglu.client.SchemaCriterion

// Apache Commons Codec
import org.apache.commons.codec.binary.Base64

// Scalaz
import scalaz._
import Scalaz._

// json4s
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.JsonMethods._

// Iglu
import iglu.client.SchemaKey
import iglu.client.Resolver

// Scala-Forex
import com.snowplowanalytics.forex.oerclient.DeveloperAccount

// Specs2
import org.specs2.mutable.Specification
import org.specs2.scalaz.ValidationMatchers

// Snowplow
import common.utils.JsonUtils
import common.enrichments.EnrichmentRegistry

class EnrichmentRegistrySpec extends Specification with ValidationMatchers {

  "EnrichmentRegistry parse" should {
    "validate all schemas" in {
      val enrichmentConfig = """|{
              |"schema": "iglu:com.snowplowanalytics.snowplow/enrichments/jsonschema/1-0-0",
              |"data": [
                |{
                  |"schema": "iglu:com.snowplowanalytics.snowplow/anon_ip/jsonschema/1-0-0",
                  |"data": {
                    |"vendor": "com.snowplowanalytics.snowplow",
                    |"name": "anon_ip",
                    |"enabled": true,
                    |"parameters": {
                      |"anonOctets": 1
                    |}
                  |}
                |},
                |{
                  |"schema": "iglu:com.snowplowanalytics.snowplow/ip_lookups/jsonschema/1-0-0",
                  |"data": {
                    |"vendor": "com.snowplowanalytics.snowplow",
                    |"name": "ip_lookups",
                    |"enabled": true,
                    |"parameters": {
                      |"geo": {
                        |"database": "GeoIPCity.dat",
                        |"uri":  "http://snowplow-hosted-assets.s3.amazonaws.com/third-party/maxmind"
                      |}
                    |}
                  |}
                |},
                |{
                  |"schema": "iglu:com.snowplowanalytics.snowplow/campaign_attribution/jsonschema/1-0-0",
                  |"data": {
                    |"vendor": "com.snowplowanalytics.snowplow",
                    |"name": "campaign_attribution",
                    |"enabled": true,
                    |"parameters": {
                      |"mapping": "static",
                      |"fields": {
                        |"mktMedium": ["utm_medium", "medium"],
                        |"mktSource": ["utm_source", "source"],
                        |"mktTerm": ["utm_term", "legacy_term"],
                        |"mktContent": ["utm_content"],
                        |"mktCampaign": ["utm_campaign", "cid", "legacy_campaign"]
                      |}
                    |}
                  |}
                |},
                |{
                  |"schema": "iglu:com.snowplowanalytics.snowplow/user_agent_utils_config/jsonschema/1-0-0",
                  |"data": {
                    |"vendor": "com.snowplowanalytics.snowplow",
                    |"name": "user_agent_utils_config",
                    |"enabled": true,
                    |"parameters": {
                    |}
                  |}
                |},
                |{
                  |"schema": "iglu:com.snowplowanalytics.snowplow/referer_parser/jsonschema/1-0-0",
                  |"data": {
                    |"vendor": "com.snowplowanalytics.snowplow",
                    |"name": "referer_parser",
                    |"enabled": true,
                    |"parameters": {
                      |"internalDomains": ["www.subdomain1.snowplowanalytics.com"]
                    |}
                  |}
                |},
                |{
                 |"schema": "iglu:com.snowplowanalytics.snowplow.enrichments/pii_enrichment_config/jsonschema/1-0-0",
                 |"data": {
                   |"vendor": "com.snowplowanalytics.snowplow.enrichments",
                   |"name": "pii_enrichment_config",
                   |"enabled": true,
                   |"parameters": {
                     |"pii": [
                       |{
                         |"pojo": {
                           |"field": "user_id"
                         |}
                       |},
                       |{
                         |"json": {
                           |"field": "contexts",
                           |"schemaCriterion": "iglu:com.mailgun/message_clicked/jsonschema/1-0-0",
                           |"jsonPath": "$.ip"
                         |}
                       |}
                     |],
                     |"strategy": {
                       |"pseudonymize": {
                         |"hashFunction": "SHA-256"
                       |}
                     |}
                   |}
                 |}
                |}
              |]
            |}""".stripMargin.replaceAll("[\n\r]", "").stripMargin.replaceAll("[\n\r]", "")

      val validatedResolver = for {
        json <- JsonUtils.extractJson(
          "",
          """{
        "schema": "iglu:com.snowplowanalytics.iglu/resolver-config/jsonschema/1-0-0",
        "data": {

          "cacheSize": 500,
          "repositories": [
            {
              "name": "Iglu Central",
              "priority": 0,
              "vendorPrefixes": [ "com.snowplowanalytics" ],
              "connection": {
                "http": {
                  "uri": "http://iglucentral.com"
                }
              }
            }
          ]
        }
      }
      """
        )
        resolver <- Resolver.parse(json).leftMap(_.toString)
      } yield resolver
      val rrc                         = new RepositoryRefConfig("test-schema", 1, List("com.snowplowanalytics.snowplow"))
      val repos                       = TestResourcesRepositoryRef(rrc, "src/test/resources/enrichment-registry-spec-schemas/")
      implicit val resolver: Resolver = new Resolver(repos = List(repos))

      val enrichmentRegistry = (for {
        registryConfig <- JsonUtils.extractJson("", enrichmentConfig)
        reg            <- EnrichmentRegistry.parse(fromJsonNode(registryConfig), true).leftMap(_.toString)
      } yield reg)

      enrichmentRegistry must beFailing
        .like {
          case failure => {
            failure must contain("error: instance failed to match exactly one schema (matched 2 out of 2)")
            // This is supposed to be failing with the above message. Because the pii_enrichment_config file contains:
            /*
              "oneOf": [
              {
                "required": [
                  "pojo"
                ]
              },
              {
                "required": [
                  "json"
                ]
              }
            ],
             */
            // in fact the iglu-scala-client validate does fail.
            // TODO: Find minimal failing test.
          }
        }

    }
  }
}
