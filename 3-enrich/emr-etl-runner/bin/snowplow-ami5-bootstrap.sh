#!/bin/sh

# Copyright (c) 2018 Snowplow Analytics Ltd. All rights reserved.
#
# This program is licensed to you under the Apache License Version 2.0,
# and you may not use this file except in compliance with the Apache License Version 2.0.
# You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the Apache License Version 2.0 is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.

# Author::        Anton Parkhomenko (mailto:support@snowplowanalytics.com)
# Copyright::     Copyright (c) 2018 Snowplow Analytics Ltd
# License::       Apache License Version 2.0

# Version::       0.2.0
# Compatibility:: AMI 5.9.0

# Fast fail
set -e

# Fix "reset by peer" crashes of RDB Loader when EMR is running behind NAT Gateway and load takes more than 10 mins
sudo /sbin/sysctl -w net.ipv4.tcp_keepalive_time=200 net.ipv4.tcp_keepalive_intvl=200 net.ipv4.tcp_keepalive_probes=5
