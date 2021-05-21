# RoseDB
[![Maintenance](https://img.shields.io/badge/Actively%20Developed%3F-Yes-green.svg)](https://GitHub.com/ShindouMihou/RoseDB/graphs/commit-activity)
[![Build Status](https://travis-ci.com/ShindouMihou/RoseDB.svg?branch=master)](https://travis-ci.com/ShindouMihou/RoseDB)
[![Release](https://img.shields.io/github/v/release/ShindouMihou/RoseDB)](https://github.com/ShindouMihou/RoseDB/releases)
[![Discord](https://img.shields.io/discord/807084089013174272?logo=Discord)](https://discord.gg/9FefYq4p83)
[![Commits](https://img.shields.io/github/last-commit/ShindouMihou/RoseDB)](https://github.com/ShindouMihou/RoseDB/commits)
[![CodeFactor](https://www.codefactor.io/repository/github/shindoumihou/rosedb/badge)](https://www.codefactor.io/repository/github/shindoumihou/rosedb)

RoseDB, a full-fledged, simple, NoSQL database created in Java with only the basic functions; 
this was created as a joke for me and also a shower thought about making something similar to MongoDB but simplified.

**How does it work?**

RoseDB works by storing data on both cache and files similar to Redis and MongoDB, for the external requests, it uses
webhooks with the requests being in JSON format.

**What are you trying to achieve with this?**

The goal of RoseDB is simplicity and speed, for example, Discord bots that only need a small database to store simple
data like prefixes and nothing sensitive.

**How secure is this?**

Currently, the security of RoseDB is basically tomato sauce; it only has one security feature which is authorization
token which is required in every request since what we wanted was a simple database that can only be accessed privately
for example Discord bots where you cannot see what the IP, Port and Authorization token is without leaking it yourself.

We do have plans on increasing security in accordance to best practices but then again, this is still a joke for me.

## How to install?

Installation of RoseDB is simple, all you need is JDK 11 (Preferably, OpenJDK 11) and some knowledge of JSON. Here are
the steps of installing RoseDB.

**Steps**
1. Download RoseDB.jar from the Releases on GitHub.
2. Place RoseDB.jar on its dedicated, empty folder.
3. Open Terminal or Powershell then execute the following line: `java -jar RoseDB.jar`
4. **OPTIONAL** CTRL + C (or exit) the application then head to the folder where you will find a config.json.
5. **OPTIONAL** Configure the JSON config as you like.

## How to configure?

Configuration of RoseDB is straightforward, here is an example of a configuration file.
```json
{
"authorization":"8a4b93a0-a6d8-4403-a44f-5cff82a537e5",
"port":5995,
"directory":"C:\\Users\\Owner\\Documents\\RoseDB\\Database\\",
"loggingLevel":"INFO"
}
```

* **Authorization**: the 'password' or 'secret' token to be sent with each request, it's a requirement
for RoseDB and there is no way to disable it with the current implementation (also have no plans), by default, the
application makes a custom one like that.
* **Port**: the port where RoseDB will run (default: 5995).
* **Directory**: the directory where RoseDB will store all the data, make sure it's dedicated to RoseDB (default: directory
where RoseDB.jar is located with "/database/" added at the end).
* **loggingLevel**: the logging level minimum that RoseDB will log (default: INFO).

## How to send requests?

Sending requests is also straightforward with the database, everything is on JSON format. 
Also, everything is sent via webhooks which means the address to use is something like: `ws://127.0.0.1:5995`

**GET REQUESTS**

To send a GET request, you can do:
```json
{
"authorization": "Authorization here", 
"method": "get",
"database": "Database here",
"collection": "Collection Here",
"identifier": "Identification of the data here."
}
```

It should reply with:
```json
{
"response":  "{//entire json data here}",
 "kode":  1
}
```

**ADD AND UPDATE REQUESTS**

* UPDATE and ADD are both the same except UPDATE uses `"method":"update"`
```json
{
"authorization": "Authorization here", 
"method": "add",
"database": "Database here",
"collection": "Collection Here",
"identifier": "Identification of the data here.",
"value":  "{//json format of the values here}"
}
```

It should reply with:
```json
{
"response":  "{//entire json data here}",
 "kode":  1
}
```

**DELETE REQUESTS**

To send a DELETE request, you can do:
```json
{
"authorization": "Authorization here", 
"method": "delete",
"database": "Database here",
"collection": "Collection Here",
"identifier": "Identification of the data here."
}
```

It should reply with:
```json
{
    "response": "Successful",
    "kode": 1
}
```

**DROP REQUESTS**

There are two ways for DROP requests, one is for dropping collections and the other for dropping database.
* To drop a database, you can leave out `collection` from the request.
* To drop a collection, you have to have both `collection` and `database` on the request.

Example of a collection drop
```json
{
    "authorization": "8a4b93a0-a6d8-4403-a44f-5cff82a537e5",
    "method": "drop",
    "database": "rose_db",
    "collection": "Mana"
}
```

It should reply with:
```json
{
    "response": "Successfully deleted the collection Mana",
    "kode": 1
}
```

Example of a database drop:
```json
{
    "authorization": "8a4b93a0-a6d8-4403-a44f-5cff82a537e5",
    "method": "drop",
    "database": "rose_db"
}
```

The expected response should be
```json
{
    "response": "Successfully deleted the database rose_db",
    "kode": 1
}
```

## Image Examples
* Collection Drop: ![collection drop](https://media.discordapp.net/attachments/731377154817916939/845257934480343040/unknown.png)
* Database Drop: ![database drop](https://media.discordapp.net/attachments/731377154817916939/845257852083109928/unknown.png)
* Delete Request: ![delete request](https://media.discordapp.net/attachments/731377154817916939/845258886307119144/unknown.png)
* Add Request: ![add request](https://media.discordapp.net/attachments/731377154817916939/845258085589319690/unknown.png)
* Get Request: ![get request](https://media.discordapp.net/attachments/731377154817916939/845258046812061736/unknown.png)

## TODO
* Create a Driver for Java
* Create a Driver for PHP
* Add more security features.
* Improve code for readability.

## Credits
* Javalin IO [javalin.io](https://javalin.io) for websockets.
* Apache Commons IO for FilenameUtils.
* Json.Json for JSON Decoding and Encoding.
