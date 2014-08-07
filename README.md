index-window
============

An Elasticsearch plugin that enables you to keep an eye on timestamped indices and only keep the N latest ones.

Create or update index window
=============================
Create an index window to keep the 5 latest indices that are prefixed by "my-index_" and they have a timestamp with the format of "yyyy-MM-dd". Do the check every 5 minutes:

<b>POST index-window?index_prefix=my-index_&date_format=yyyy-MM-dd&keep=5&check_interval=5m</b>

Note: the key of an index window is the index_prefix, so you cannot have multiple windows defined on the same index prefix.

Delete an index window
======================
Delete the index window defined for indices with prefix "my-index_":<br>
<b>DELETE index-window/my-index_</b>

Default values
==============
date_format = yyyy.MM.dd<br>
keep = 7<br>
check_interval = 30m<br>


Install
=======

- Build the project by this command:<br>
<b>mvn assembly:assembly</b>

- take the created jar file in the "target" directory, called "index-window-<version>-jar-with-dependencies.jar"

- install the jar file as any other plugin on your Elasticsearch node
