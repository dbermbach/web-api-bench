# web-api-bench
This project can be used to run long-term availability and performance tests for web APIs that are available over http(s). Currently, only http(s) GET requests are supported but using POST, PUT, etc. requires only a few changes (statically determine the content of a requests and simply set the URLConnection instance to use POST, PUT, etc.

Features:
* send requests and log results
* stable enough for long-term testing without crashes
* support for http GET, https GET, ping, and cipherscan (https://github.com/jvehent/cipherscan)

Setup:
* put URLs/domain addresses in config files similar to the examples in the folder sampleInputFiles (please, make sure that the respective target "speaks" the desired protocol)
* specify the location of those files as program parameters and run it
* preferably, choose a geo-distributed deployment
* you can use the following analysis scripts afterwards (to be added)

Dependencies:
* no Java library dependencies (that's why this is not a maven project :))
* install cipherscan into the same folder (or change the hard-coded path in class CipherscanRunner, line 81), you can simply clone the cipherscan git from: https://github.com/jvehent/cipherscan
