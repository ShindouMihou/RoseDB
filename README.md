![RoseDB](https://cdn.mihou.pw/rosedb.png)

[![Maintenance](https://img.shields.io/badge/Actively%20Developed%3F-Yes-green.svg)](https://GitHub.com/ShindouMihou/RoseDB/graphs/commit-activity)
[![Build Status](https://travis-ci.com/ShindouMihou/RoseDB.svg?branch=master)](https://travis-ci.com/ShindouMihou/RoseDB)
[![Release](https://img.shields.io/github/v/release/ShindouMihou/RoseDB)](https://github.com/ShindouMihou/RoseDB/releases)
[![Discord](https://img.shields.io/discord/807084089013174272?logo=Discord)](https://discord.gg/9FefYq4p83)
[![Commits](https://img.shields.io/github/last-commit/ShindouMihou/RoseDB)](https://github.com/ShindouMihou/RoseDB/commits)
[![CodeFactor](https://www.codefactor.io/repository/github/shindoumihou/rosedb/badge)](https://www.codefactor.io/repository/github/shindoumihou/rosedb)
![GitHub](https://img.shields.io/github/license/ShindouMihou/RoseDB)

RoseDB is a simple, NoSQL database that is written completely in Java containing the most basic functions that is needed for a database.
This project was initially created as a random project for me (a shower thought) but has evolved into a learning experience for me.

## Table of Contents
- [How does it work](#how-does-it-work)
- [Goal](#goal)
- [Security](#security)
- [Installation](#installation)
- [Configuration](#configuration)
- [Wrappers](#wrappers)
- [Sending Requests](#sending-requests)
  * [Notice](#notice)
  * [Content-Type](#content-type)
- [Get Request](#get-request)
  * [Response](#response)
- [Aggregate Requests](#aggregate-requests)
  * [Collection Request](#collection-request)
  * [Collection Response](#collection-response)
  * [Collection Example Response](#collection-example-response)
  * [Database Request](#database-request)
  * [Database Response](#database-response)
  * [Database Sample Response](#database-sample-response)
- [Add and Update Requests](#add-and-update-requests)
  * [Update Single Field Request](#update-single-field-request)
  * [Update Multiple Fields Request](#update-multiple-fields-request)
  * [Example of Multiple Fields Update Request](#example-of-multiple-fields-update-request)
  * [Add and Update Response](#add-and-update-response)
- [Delete Request](#delete-request)
  * [Delete Item Request](#delete-item-request)
  * [Delete Item Response](#delete-item-response)
  * [Delete Item Example Response](#delete-item-example-response)
  * [Delete Key or Field Request](#delete-key-or-field-request)
  * [Delete Multiple Keys or Fields Request](#delete-multiple-keys-or-fields-request)
  * [Delete Field and Fields Response](#delete-field-and-fields-response)
  * [Example of Delete Field and Fields Response](#example-of-delete-field-and-fields-response)
- [Drop Requests](#drop-requests)
  * [Collection Request](#collection-drop-request)
  * [Collection Response](#collection-drop-response)
  * [Collection Example Response](#collection-drop-example-response)
  * [Database Request](#database-drop-request)
  * [Database Response](#database-drop-response)
  * [Database Example Response](#database-drop-response-example)
- [Revert Request](#revert-request)
  * [Example](#example)
  * [Response](#response-1)
  * [Example](#example-1)
  * [Limitations](#limitations)

## How does it work
RoseDB works with both in-memory and file data storage, for every request it receives, it stores it on a queue and also on its cache which
will be saved immediately at an interval of 5 seconds to the specified directory. It utilizes websockets to receive and send data to clients
and should be more than capable to process plenty of requests per second.

## Goal
My primary goal/aim of this project is not to create the best database but a simple database that can get you up and running in literal mere seconds
with little to no configuration at all.

Are you not convinced? Have a look at our no-configuration setup.
1. Download the jar from Releases.
2. Run the jar from a console: `java -jar RoseDB.jar`.
3. Open the `config.json` and get the `Authorization` value.
4. Install one of our drivers (for example, the official Java driver) and follow the isntructions to use the driver.

## Security
RoseDB's current security features is a bit lacking, other than the Authorization header, there is currently no other security feature but
we are focusing our attention to bringing more security features onto the application but since we are still in our very early stages,
we are trying to get everything up and running first before focusing on security.

Though, in my opinion, RoseDB is more suited to be used in simple applications like tiny Discord bots that is shared among friends and not 
large applications that require super complicated features, after all, the main aim of RoseDB is to be as simple as possible and that involves
replication and load balancing (future).

## Requirements
* JDK 11 (Preferably, OpenJDK 11).
* An computer with storage, memory and a terminal.
* A keyboard that you can type on.
* Internet Connection (to download the JAR file, naturally).

## Installation

Installation of RoseDB is simple, all you need is JDK 11 (Preferably, OpenJDK 11) and tier-one extreme basic knowledge of JSON. Here are
the steps of installing RoseDB.
1. Download RoseDB.jar from the Releases on GitHub.
2. Place RoseDB.jar on its dedicated, empty folder.
3. Open Terminal or Powershell then execute the following line: `java -jar RoseDB.jar`
4. **OPTIONAL** CTRL + C (or exit) the application then head to the folder where you will find a config.json.
5. **OPTIONAL** Configure the JSON config as you like.
6. **OPTIONAL** Run the jar file again with the same line: `java -jar RoseDB.jar`

## Configuration
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

## Wrappers
If you want to quickly get up and running with your application then feel free to use our wrappers.
* [Official Java Wrapper](https://github.com/ShindouMihou/Rose-Java-Driver)
* [Python Wrapper](https://github.com/LittleCrowRevi/Python-RoseDB-Driver)

## Sending Requests

Sending requests is also straightforward with the database, everything is on JSON format. 
Also, everything is sent via webhooks which means the address to use is something like: `ws://127.0.0.1:5995`

### Notice
All requests towards RoseDB v1.1.0 must have the Authorization header which is validated at connection, this is not something
that will be backward compatible since we want to enforce this as soon as possible.

### Content-Type
All requests must be made in the Content-Type `JSON`, please keep this in mind.

## Get Request
The `method` field for this must be `get`.

| FIELD      	| TYPE   	| DESCRIPTION                                                                         	|
|------------	|--------	|-------------------------------------------------------------------------------------	|
| method     	| string 	| The type of request to send (GET/ADD/DELETE/REVERT/AGGREGATE/ETC)                   	|
| database   	| string 	| The database that holds the item.                                                   	|
| collection 	| string 	| The collection that holds the item.                                                 	|
| identifier 	| string 	| The item's identifier name.                                                         	|
| unique     	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Response

|   FIELD  	|   TYPE  	|                                             DESCRIPTION                                             	|
|:--------:	|:-------:	|:---------------------------------------------------------------------------------------------------:	|
| response 	|   json  	| The response from the server, it will always be JSON for specific methods like (GET, ADD, UPDATE).  	|
|   kode   	| integer 	|           The server reply code (1 for success, 0 for no results, -1 for invalid request).          	|
|  replyTo 	|  string 	|                              The unique code sent back (for callbacks).                             	|

## Aggregate Requests

The `method` field for this must be `aggregate`.
There are two types of aggregation requests (collection-level and database-level aggregation), both of which has a similar request.

### Collection Request
|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                              The database to aggregate.                          	    |
| collection 	| string 	|                             The collection to aggregate.                         	    |
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Collection Response

|          FIELD         	|     TYPE    	|                                    DESCRIPTION                                   	|
|:----------------------:	|:-----------:	|:--------------------------------------------------------------------------------:	|
| {{name of collection}} 	| nested json 	|                Replies with a JSON object that has all the items.                	|
|          kode          	|   integer   	| The server reply code (1 for success, 0 for no results, -1 for invalid request). 	|
|         replyTo        	|    string   	|                    The unique code sent back (for callbacks).                    	|

### Collection Example Response
```json
{
    "kode": 1,
    "replyTo": "Unique Identifier Here",
    "test": {
        "test": "{\"item\":1}",
        "test2": "{\"item\":2}",
        "test3": "{\"item\":3}"
    }
}
```

### Database Request
|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                              The database to aggregate.                              	|
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Database Response
|         FIELD        	|         TYPE         	|                                    DESCRIPTION                                   	|
|:--------------------:	|:--------------------:	|:--------------------------------------------------------------------------------:	|
| {{name of database}} 	| multiple nested json 	|                Replies with a JSON object that has all the items.                	|
|         kode         	|        integer       	| The server reply code (1 for success, 0 for no results, -1 for invalid request). 	|
|        replyTo       	|        string        	|                    The unique code sent back (for callbacks).                    	|

### Database Sample Response
```json
{
    "kode": 1,
    "replyTo": "Unique Identifier Here",
    "rose_db": {
        "test": {
            "test": "{\"item\":1}",
            "test2": "{\"item\":2}",
            "test3": "{\"item\":3}"
        },
        "tomato": {
            "tomato": "{\"item\":1}"
        }
    }
}
```

## Add and Update Requests
The `method` field for this must be `add` or `update`.

|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                           The database to store the item.                           	|
| collection 	| string 	|                          The collection to store the item.                          	|
| identifier 	| string 	|                           The identifier name of the item.                          	|
|    value   	| json   	|                        The value of the item (JSON) required.                       	|
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Update Single Field Request
|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                           The database to store the item.                           	|
| collection 	| string 	|                          The collection to store the item.                          	|
| identifier 	| string 	|                           The identifier name of the item.                          	|
|     key    	| string 	|                            The name of the key to update.                           	|
|    value   	|   any  	|                             The value to set on the key.                            	|
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Update Multiple Fields Request
|   FIELD    	|       TYPE      	|                                     DESCRIPTION                                     	|
|:----------:	|:---------------:	|:-----------------------------------------------------------------------------------:	|
|   method   	|      string     	|                             The type of request to send.                            	|
|  database  	|      string     	|                           The database to store the item.                           	|
| collection 	|      string     	|                          The collection to store the item.                          	|
| identifier 	|      string     	|                           The identifier name of the item.                          	|
|    keys    	| array of string 	|                           The names of the keys to update.                          	|
|   values   	|   array of any  	|                            The values to set on the keys.                           	|
|   unique   	|      string     	| The unique value to return (used for getting back exact responses from the server). 	|

### Example of Multiple Fields Update Request
```json
{
    "method": "update",
    "database": "Database Name",
    "collection": "Collection Name",
    "identifier": "Item Name",
    "key": [
        "id",
        "code",
        "file"
    ],
    "value": [
        "1",
        "valuable",
        "textfile"
    ],
    "unique": "255asd2"
}
```

### Add and Update Response
|   FIELD  	|   TYPE  	|                                    DESCRIPTION                                   	|
|:--------:	|:-------:	|:--------------------------------------------------------------------------------:	|
| response 	|   json  	|            Replies back with the same JSON value from the value field.           	|
|   kode   	| integer 	| The server reply code (1 for success, 0 for no results, -1 for invalid request). 	|
|  replyTo 	|  string 	|                    The unique code sent back (for callbacks).                    	|

## Delete Request
The `method` field must be `delete` for this request.

### Delete Item Request
|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                           The database to store the item.                           	|
| collection 	| string 	|                          The collection to store the item.                          	|
| identifier 	| string 	|                      The identifier name of the item to remove.                     	|
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Delete Item Response
|   FIELD  	|   TYPE  	|                                       DESCRIPTION                                      	|
|:--------:	|:-------:	|:--------------------------------------------------------------------------------------:	|
| response 	|   json  	| Replies back with something among the likes of: "The item {{identifier}} was deleted." 	|
|   kode   	| integer 	|    The server reply code (1 for success, 0 for no results, -1 for invalid request).    	|
|  replyTo 	|  string 	|                       The unique code sent back (for callbacks).                       	|

### Delete Item Example Response
```json
{
    "response": "The item [identifier] was deleted.",
    "kode": 1,
    "replyTo": "Unique identifier from request"
}
```

### Delete Key or Field Request
|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                           The database to store the item.                           	|
| collection 	| string 	|                          The collection to store the item.                          	|
| identifier 	| string 	|                      The identifier name of the item to remove.                     	|
|     key    	| string 	|                         The key name of the field to remove.                        	|
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Delete Multiple Keys or Fields Request
|   FIELD    	|       TYPE      	|                                     DESCRIPTION                                     	|
|:----------:	|:---------------:	|:-----------------------------------------------------------------------------------:	|
|   method   	|      string     	|                             The type of request to send.                            	|
|  database  	|      string     	|                           The database to store the item.                           	|
| collection 	|      string     	|                          The collection to store the item.                          	|
| identifier 	|      string     	|                      The identifier name of the item to remove.                     	|
|    keys    	| array of string 	|                        The key names of the fields to remove.                       	|
|   unique   	|      string     	| The unique value to return (used for getting back exact responses from the server). 	|

### Delete Field and Fields Response
|   FIELD  	|   TYPE  	|                                    DESCRIPTION                                   	|
|:--------:	|:-------:	|:--------------------------------------------------------------------------------:	|
| response 	|   json  	|                 Replies back with the new JSON value of the item.                	|
|   kode   	| integer 	| The server reply code (1 for success, 0 for no results, -1 for invalid request). 	|
|  replyTo 	|  string 	|                    The unique code sent back (for callbacks).                    	|

### Example of Delete Field and Fields Response
```json
{
    "response": "{\"id\":\"1.2\"}",
    "kode": 1,
    "replyTo": "Unique String"
}
```

### Drop Requests

There are two ways for DROP requests, one is for dropping collections and the other for dropping database.
* To drop a database, you can leave out `collection` from the request.
* To drop a collection, you have to have both `collection` and `database` on the request.

The `method` field for this request must be `drop`.

### Collection Drop Request
|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                          The database where the collection is located.                |
| collection 	| string 	|                               The collection to drop.                                 |
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Collection Drop Response

|   FIELD  	|   TYPE  	|                                              DESCRIPTION                                             	|
|:--------:	|:-------:	|:----------------------------------------------------------------------------------------------------:	|
| response 	|  string 	| Replies back with something among the likes of: "Successfully deleted the collection {{collection}}" 	|
|   kode   	| integer 	|           The server reply code (1 for success, 0 for no results, -1 for invalid request).           	|
|  replyTo 	|  string 	|                              The unique code sent back (for callbacks).                              	|

### Collection Drop Example Response
```json
{
    "response": "Successfully deleted the collection Mana",
    "kode": 1,
    "replyTo": "Unique Identifier from Request"
}
```

### Database Drop Request
|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                                 The database to drop.                              	  |
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Database Drop Response
|   FIELD  	|   TYPE  	|                                              DESCRIPTION                                             	|
|:--------:	|:-------:	|:----------------------------------------------------------------------------------------------------:	|
| response 	|  string 	| Replies back with something among the likes of: "Successfully deleted the database {{database}}" 	    |
|   kode   	| integer 	|           The server reply code (1 for success, 0 for no results, -1 for invalid request).           	|
|  replyTo 	|  string 	|                              The unique code sent back (for callbacks).                              	|

### Database Drop Response Example
```json
{
    "response": "Successfully deleted the database rose_db",
    "kode": 1,
    "replyTo": "Unique Identifier from Request"
}
```

## Revert Request

The `method` field to use for this is `revert`.

You can revert an add or update request by simply sending a revert request to the server which looks like:

|   FIELD    	|  TYPE  	|                                     DESCRIPTION                                     	|
|:----------:	|:------:	|:-----------------------------------------------------------------------------------:	|
|   method   	| string 	|                             The type of request to send.                            	|
|  database  	| string 	|                           The database to store the item.                           	|
| collection 	| string 	|                          The collection to store the item.                          	|
| identifier 	| string 	|                      The identifier name of the item to revert.                     	|
|   unique   	| string 	| The unique value to return (used for getting back exact responses from the server). 	|

### Example
```json
{
  "method": "revert",
  "database": "Database Name",
  "collection": "Collection Name",
  "idenitifer": "Item Name",
  "unique": "Unique String Here"
}
```

### Response
|   FIELD  	|   TYPE  	|                                    DESCRIPTION                                   	|
|:--------:	|:-------:	|:--------------------------------------------------------------------------------:	|
| response 	|   json  	|            Replies back with the same JSON value from the value field.           	|
|   kode   	| integer 	| The server reply code (1 for success, 0 for no results, -1 for invalid request). 	|
|  replyTo 	|  string 	|                    The unique code sent back (for callbacks).                    	|

### Example
The response of this request will be the last version of the file.
```json
{
  "response": "{\"version\":1}",
  "kode": 1,
  "replyTo": "Unique ID here."
}
```

### Limitations
* The versions are saved on application-level cache and is dumped when the application is closed normally and not abruptly.
* Each item is limited to one version and will be overriden each time an *ADD* or *UPDATE* request is sent that will override the item.

## Image Examples
* Collection Drop: 

![collection drop](https://media.discordapp.net/attachments/731377154817916939/845257934480343040/unknown.png)

* Database Drop: 

![database drop](https://media.discordapp.net/attachments/731377154817916939/845257852083109928/unknown.png)

* Delete Request: 

![delete request](https://media.discordapp.net/attachments/731377154817916939/845258886307119144/unknown.png)

* Add Request

![add request](https://media.discordapp.net/attachments/731377154817916939/845258085589319690/unknown.png)

* Get Request: 

![get request](https://media.discordapp.net/attachments/731377154817916939/845258046812061736/unknown.png)

* Aggregate Request 

![aggregate request](https://media.discordapp.net/attachments/775601335931240459/846046876548595782/unknown.png)

## TODO
* Add more security features.
* Improve code for readability.

## Maintainers
[Shindou Mihou](https://github.com/ShindouMihou)

## Credits
* [Javalin IO](https://javalin.io) for websockets.
* [Apache Commons IO](https://commons.apache.org/) for FilenameUtils.
* [org.json](https://mvnrepository.com/artifact/org.json/json/20210307) for JSON Decoding and Encoding.
