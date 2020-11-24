# Follower Maze Code Challenge - Soundcloud test

## Part 1

My main focus was on the project's abstraction and reorganization for extensibility, readability, maintainability and encapsulation, for those aspects:

Since the code was all in one file without any abstraction and duplicated code, it was very hard to unit test anything, so separation of concerns was a priority, with the new design all pieces of code can be validated, extended and understood in a better way.

I decided to abstract the whole action, so now each action is isolated and much easier to change, but they share some things which are common so we do not duplicate code. That was the biggest change.

Also all async jobs running in the global context is not ideal, so I simply separated the contexts for increased fault-tolerance, the trade-off is that we increase communication cost between contexts.

I didn't want to change the Await usage as we do not want this to become ready to production, and ideally in production we would use a framework that know how to handle concurrency with websockets. Most likely using Akka and so on.

## Part 2

I assumed that malformed input was something wrong in the body of the particular event, and not on the initial splittable string, cause that would most likely be treated by a framework.

Another assumption for the requirement "target user is not connected" was that if user is in the clientPool TrieMap. That facilitates testing as you don't really need to create/mock a socket. Ideally either this would be 100% reliable or we would put that check on the socket call and mock it in tests.   

The error and DLQ structures are as simple as possible, but easily extensible. In production we would publish those messages to an asynchronous queue such as Amazon SQS, a RabbitMQ queue, a kafka topic or even a redis instance could be OK, where we could check and reprocess later, we could create a routine that checks the queue and delivers to previously disconnected users their message whenever they get back online, for malformed messages we should check, then either adapt somehow to the malformed messages, or simply a routine that deletes them if we can't do anything about. Lastly, about kinds we don't yet support we can either store in the queue for later processing when we support or just discard.


## Comments

To check how Part 1 was built before Part 2 was started, just checkout that previous commit:
 
commit dcb96d223895c209a50f9105bcf35745a5f7ac44 (origin/master, origin/HEAD)
Author: codintestscloud <grufino1@hotmail.com>
Date:   Mon Nov 23 19:00:33 2020 +0100

    refactor complete
    
    
Finally, to make this code production-ready a lot would have to change. I would personally handle side-effects in a more functional and controllable way using cats effect, and execution contexts/concurrency using akka. Also we need to make environment configurations so that we can deploy to more than one machine, and also a socket connection pool could be useful for accepting more connections. Those are the top priority in my opinion.
