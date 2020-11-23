# Follower Maze Code Challenge - Soundcloud test

## Part 1

### The approach you took and reasoning you applied to arrive at your final solution.

My main focus was on the project's abstraction and reorganization for extensibility, readability, maintainability and encapsulation, for those aspects:

Since the code was all in one file without any abstraction and duplicated code, it was very hard to unit test anything, so separation of concerns was a priority, with the new design all pieces of code can be validated, extended and understood in a better way.

I decided to abstract the whole action, so now each action is isolated and much easier to change, but they share some things which are common so we do not duplicate code. That was the biggest change.

Also all async jobs running in the global context is not ideal, so I simply separated the contexts, without much attention to performance right now.

I didn't want to change the Await usage as we do not want this to become ready to production, and ideally in production we would use a framework that know how to handle concurrency with websockets. Most likely using Akka and so on.   