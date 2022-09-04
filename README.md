## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!


### My solution

At a first look at the challenge the main problem to solve here is how we schedule the payments, doing a bit of research online think we can have 2 approaches to solve this problem:

1) Using some sort of async messages queue, like Kafka, RabbitMQ, etc. This approach will basically require that on invoice creation we will need to send a message to the queue this should be processed at a certain time and then once everything is fine then the consumer would need to reschedule it if the payment fails.
The advantage to this method is that we can more easily scale the consumers to the messages/invoices. The disadvantage is that this solution is a lot more complicated and requires a more infrastructure to be setup.
This sadly doesn't work out of the box since processing messages usually happens real time, but we need them to only be processed on a certain day/schedule which would require some sort of intermediary service, like the one described here for example https://medium.com/@fkarakas/kafka-message-scheduler-69823cc62f8c#:~:text=A%20schedule%20is%20just%20a,specific%20headers%20and%20a%20payload.&text=The%20scheduler%20reads%20the%20topic,planned%20for%20the%20current%20day.

2) Using a recurring job that will start running on the first day of the month and will process all the invoices that are due to be paid. This approach is the one I decided to go with since it's simpler and it doesn't require any external service to be running and fits in the time available for the challenge.

Observations:

- Ideally the invoices should be country and currency based as you can have cases where you need to use a different payment provider for different countries (for example in Serbia you can't have the money leave the country)
and countries can change currency(f.ex. Croatia at the end of this year). Decided to ignore this for now, in order to save a bit of time.
- Because there can be some error cases that can't be treated automatically, such as missing customer, wrong currency etc. decided to add a FAILED invoice status, which will then have to be treated manually.
