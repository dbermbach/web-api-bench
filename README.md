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

For researchers:
* If you want to reproduce the results of our paper, you can find our original raw data here: https://github.com/ErikWittern/web-api-benchmarking-data
* If you use this tool for research purposes with a resulting publication, we would appreciate if you cite our paper:
```TeX
@inproceedings{bermbach_wittern_web_api_bench,
 author = {David Bermbach and Erik Wittern},
 title = {Benchmarking Web API Quality},
 booktitle = {Proceedings of the 16th International Conference on Web Engineering (ICWE2016)},
 series = {ICWE'16},
 year = {2016},
 publisher = {Springer}
}
```
