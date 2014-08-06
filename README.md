index-window
============

An Elasticsearch plugin that enables you to keep an eye on timestamped indices and only keep the N latest ones.

Create or update index window
=============================
Create an index window to keep the 5 latest indices that are prefixed by "my-index_" and they have a timestamp with the format of "yyyy-MM-dd". Do the check every 5 minutes:
POST index-window?index_prefix=my-index_&date_format=yyyy-MM-dd&keep=5&check_interval=5m

Note: the key of an index window is the index_prefix, so you cannot have multiple windows defined on the same index prefix.

Delete an index window
======================
Delete the index window defined for indices with prefix "my-index_":
DELETE index-window/my-index_

Default values
==============
date_format = yyyy-MM-dd
keep = 5
check_interval = 5m
