# MongoSchema
[![Build status](https://travis-ci.org/scf37/mongoschema.svg?branch=master)](https://travis-ci.org/scf37/dbinstaller)

A tool to analyze mongoDB collections and output schema

##usage
0. have JRE8 on your system
1. wget https://raw.githubusercontent.com/scf37/mongoschema/master/ms && chmod +x ms
2. ./ms --help
3. read command-line help and hack away

##Output example

```
local.startup_log
Object
  _id: String(20)
  hostname: String(6)
  startTime: DateTime
  startTimeLocal: String(23)
  cmdLine: Object
    net: Object
      bindIp: String(9)
    storage: Object
      dbPath: String(10)
    systemLog: Object
      destination: String(4)
      path: String(21)
  pid: Int64
  buildinfo: Object
    version: String(5)
    gitVersion: String(40)
    modules: Array(0) of Unknown
    allocator: String(8)
    javascriptEngine: String(5)
    sysInfo: String(10)
    versionArray: Array(4) of Int32
    openssl: Object
      running: String(25)
      compiled: String(25)
    buildEnvironment: Object
      distmod: String(10)
      distarch: String(6)
      cc: String(46)
      ccflags: String(279)
      cxx: String(46)
      cxxflags: String(50)
      linkflags: String(49)
      target_arch: String(6)
      target_os: String(5)
    bits: Int32
    debug: Boolean
    maxBsonObjectSize: Int32
    storageEngines: Array(4) of String(16)

test.contacts
Object
  _id: ObjectId
  phone: String(5)
  email: String(14)
  status: String(7) of Enum('Ok', 'Failed', '')
  test: Optional String(4)

web.user
Object
  _id: Int64
  email: String(38)

web3.dbscripts
Empty collection
```
