![RoseDB](https://cdn.mihou.pw/rosedb.png)

[![Maintenance](https://img.shields.io/badge/Actively%20Developed%3F-Yes-green.svg)](https://GitHub.com/ShindouMihou/RoseDB/graphs/commit-activity)
[![Build Status](https://travis-ci.com/ShindouMihou/RoseDB.svg?branch=master)](https://travis-ci.com/ShindouMihou/RoseDB)
[![Release](https://img.shields.io/github/v/release/ShindouMihou/RoseDB)](https://github.com/ShindouMihou/RoseDB/releases)
[![Discord](https://img.shields.io/discord/807084089013174272?logo=Discord)](https://discord.gg/9FefYq4p83)
[![Commits](https://img.shields.io/github/last-commit/ShindouMihou/RoseDB)](https://github.com/ShindouMihou/RoseDB/commits)
[![CodeFactor](https://www.codefactor.io/repository/github/shindoumihou/rosedb/badge)](https://www.codefactor.io/repository/github/shindoumihou/rosedb)
![GitHub](https://img.shields.io/github/license/ShindouMihou/RoseDB)

## ❤️ What is RoseDB?
RoseDB is a simple, NoSQL database that is written completely in Java containing the most basic functions that is needed for a database.
This project was initially created as a random project for me (a shower thought) but has evolved into a learning experience for me.

## ⚙️ How does it work
RoseDB works with both in-memory and file data storage, for every request it receives, it stores it on a queue and also on its cache which
will be saved immediately at an interval of 5 seconds to the specified directory. It utilizes websockets to receive and send data to clients
and should be more than capable to process plenty of requests per second.

## ✨ Goal
My primary goal/aim of this project is not to create the best database but a simple database that can get you up and running in literal mere seconds
with little to no configuration at all.

Are you not convinced? Have a look at our no-configuration setup.
1. Download the jar from Releases.
2. Run the jar from a console: `java -jar RoseDB.jar`.
3. Open the `config.json` and get the `Authorization` value.
4. Install one of our drivers (for example, the official Java driver) and follow the isntructions to use the driver.

## 🛡️ Security
RoseDB has support for websocket SSL which counts as security and also an `Authorization` header enforcement with the header being compared with hash (it is written to the disk as a hash value for security), there will be more security features and if you have more to suggest then feel free to send a Pull Request or an Issue explaining everything. We are always focusing our attention to bringing more security features onto the application but since we are still in our very early stages,
we are trying to get everything up and running first before focusing on security.

Though, in my opinion, RoseDB is more suited to be used in simple applications like tiny Discord bots that is shared among friends and not 
large applications that require super complicated features, after all, the main aim of RoseDB is to be as simple as possible and that involves
replication and load balancing (future).

## 🖥️ Requirements
* JDK 11 (Preferably, OpenJDK 11).
* An computer with storage, memory and a terminal.
* A keyboard that you can type on.
* Internet Connection (to download the JAR file, naturally).

## 🖱️ Installation

Installation of RoseDB is simple, all you need is JDK 11 (Preferably, OpenJDK 11) and tier-one extreme basic knowledge of JSON. Here are
the steps of installing RoseDB.
1. Download RoseDB.jar from the [Releases on GitHub](https://github.com/ShindouMihou/RoseDB/releases).
2. Place RoseDB.jar on its dedicated, empty folder.
3. Open Terminal or Powershell then execute the following line: `java -jar RoseDB.jar`
4. **OPTIONAL** CTRL + C (or exit) the application then head to the folder where you will find a config.json.
5. **OPTIONAL** Configure the JSON config as you like.
6. **OPTIONAL** Run the jar file again with the same line: `java -jar RoseDB.jar`

## 📝 Configuration
Configuration of RoseDB is straightforward, here is an example of a configuration file (it is on `JSON` format).
|            FIELD           	|   TYPE  	|                                                        DESCRIPTION                                                        	|        DEFAULT VALUE        	|
|:--------------------------:	|:-------:	|:-------------------------------------------------------------------------------------------------------------------------:	|:---------------------------:	|
|        Authorization       	|  string 	|                          The authorization to use to validate incoming connections and requests.                          	|       A random string.      	|
|            Cores           	| integer 	|                                      The number of cores the application should use.                                      	|              1              	|
| maxTextMessageBufferSizeMB 	| integer 	|                          The maximum message buffer size for each message (request) received (MB)                         	|              5              	|
|    maxTextMessageSizeMB    	| integer 	|                                  The maximum text (message/request) size to receive (MB)                                  	|              5              	|
|            port            	| integer 	|                                              The port that RoseDB should use.                                             	|             5995            	|
|         versioning         	| boolean 	|                 Whether RoseDB should save a backup version for all items that are modified. (Recommended)                	|             true            	|
|           preload          	| boolean 	|                         Whether to preload all items that are saved on the database. (Recommended)                        	|             true            	|
|        updateChecker       	| boolean 	|                      Whether to check for RoseDB updates from the maintainer's server. (Recommended)                      	|             true            	|
|          directory         	|  string 	|                              The exact directory folder where RoseDB will save all the data.                              	| running location of the jar 	|
|  heartbeatIntervalSeconds  	| integer 	|                 The interval seconds of when the server should send a heartbeat packet to all connections.                	|              30             	|
|        loggingLevel        	|  string 	| The minimum level of which RoseDB should log (recommended at INFO for performance), options: INFO, WARNING, DEBUG, ERROR. 	|             INFO            	|

## 💌  Wrappers
If you want to quickly get up and running with your application then feel free to use our wrappers.
* [Official Java Wrapper](https://github.com/ShindouMihou/Rose-Java-Driver)
* [Python Wrapper](https://github.com/LittleCrowRevi/Python-RoseDB-Driver)

## 🌠 How simple is RoseDB?
RoseDB is very simple and easy to use, after following [Installation](#installation), you can quickly get up and running by sending requests
to the server via `Queria` format or `JSON` format.

An example of a `Queria` **GET** request is:
```
database.collection.get(item)
```

An example of a `Queria` **ADD** request is:
```
database.collection.add(item, {"someKey":"someValue"})
```

Are you interested, learn more at our [GitHub Wiki](https://github.com/ShindouMihou/RoseDB/wiki).

## ❤️‍🔥 Reporting a Vulnerability

To report a vulnerability, simply file an issue at [Issue Template](https://github.com/ShindouMihou/RoseDB/issues/new?assignees=ShindouMihou&labels=bug&template=bug_report.md&title=).

## 🚀 Add a suggestion

To suggest a new feature or some sort, feel free to send a suggestion issue at [Suggestion Template](https://github.com/ShindouMihou/RoseDB/issues/new?assignees=&labels=&template=feature_request.md&title=)

# 🌟 Maintainers
[Shindou Mihou](https://github.com/ShindouMihou), creator and developer.

# 💫 Credits
* [Bucket4j](https://github.com/vladimir-bukhtoyarov/bucket4j) for Rate-limiter.
* [TooTallNate](https://github.com/TooTallNate/Java-WebSocket) for Websocket (replacing `io.javalin`).
* [Resilience4j](https://github.com/resilience4j/resilience4j) for Retry and Timeouts.
* [Apache Commons](https://commons.apache.org/) for FilenameUtils, Hashing, FileUtils, Hash Validation.
* [org.json](https://mvnrepository.com/artifact/org.json/json/20210307) for JSON Decoding and Encoding.
* [GSON](https://github.com/google/gson) for JSON Serialization and Deserialization (working in tandem with `org.json`)
