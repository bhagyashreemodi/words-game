## Lab 0 - A Game about Words, Java version

### Game Design Narrative
* I started by reading through the lab description and test cases thoroughly to understand the overall requirements 
and expected functionality.
* I then decided to implement 1 command handler for each command as a separate method and delegate the logic specific
to executing that command to the appropriate method.
* I then began implementing each command handler method separately, like executeHello(), executeClose(), etc. 
* I tested each one independently to ensure it behaved as expected before moving on to the next.
* The GameServer class maintains two ConcurrentHashMaps to store the centralized state of all games and players. 
Using concurrent data structures ensures thread-safety as multiple player threads access and modify the maps.
* The GameThread class handles all communication with each player connection. 
It contains the command handler methods that parse the incoming messages and perform the appropriate actions by 
interacting with the GameServer maps and objects.
* The Game and Player classes encapsulate related state and behaviors for games and players. They provide methods to 
support operations needed by the GameThread handlers.
* I structured the code with proper partitioning of classes, extracted reusable utilities like respond(), and provided 
comments for readability. The single file approach keeps it simple while still maintaining good organization and 
avoiding duplication.
* Overall, I focused on thread-safety, re-usability, readability, and correctness by leveraging concurrency constructs 
and testing each component thoroughly along the way. This iterative approach helped build up the full functionality in 
a robust manner.
* More details about the implementation are provided in the javadocs for each class and method.
### Discussion of failures
#### Failures handled by the implementation
Handled all the errors and exceptions as per the lab description and test cases.
#### Failures not handled by the implementation
If the leader player sends an extremely large message/data payload during FILE_UPLOAD that exceeds the server's 
available memory when trying to load it into a String, it could cause an OutOfMemoryError.

#### Note
I have updated the docs command in the Makefile with -private option to generate javadocs for all the inner 
classes and private members.I have included the output of make docs command in the doc folder for final submission.
### Getting started

This repository includes several Java packages arranged in different directories.  This file is located
in the main working directory, and your IDE should point to this directory to run your code; similarly, if 
you run code from the command line, your terminal should be working out of this directory. We're not using
any special libraries, just stock Java APIs.

If you're setting up a basic environment for Java labs in this course, a default Ubuntu Linux installation 
would only require you to do
```
sudo apt update; sudo apt -y upgrade
sudo apt install git openjdk-21-jdk openjdk-21-jre make
```
to start working on the lab. Everything else is controlled by `make` using the `javac`, `java`, and `javadoc`
utilities.


### Initial repository contents

The top-level directory (called `lab0-java` here) of the initial starter-code repository includes four things:
* This `README.md` file
* The Lab 0 `Makefile`, described in detail later
* The `test` directory that contains the Lab 0 autograder, which you should not modify
* The `gameServer` source directory, which is where all your work will be done

Visually, this looks roughly like the following, with the `test` directory compressed for clarity:
```
\---lab0-java
        +---gameServer
        |   +---GameServer.java
        |   \---package-info.java
        +---test
        |   +---gameServer
        |   |   +---TestGame.java
        |   |   +---TestGameClient.java
        |   |   +---TestCheckpoint_*.java  (4x in total)
        |   |   +---TestFinal_*.java       (6x in total)
        |   |   \---package-info.java
        |   +---util [contains generic test suite]
        |   +---Lab0CheckpointTests.java
        |   +---Lab0FinalTests.java
        |   +---Lab0Tests.java
        |   +---test.txt
        |   \---package-info.java
        +---Makefile
        \---README.md
```
The details of each of these will hopefully become clear after reading the rest of this file.


### Creating your GameServer

The `gameServer` package initially only includes a skeleton of the game server implementation for Lab 0. Your
primary task in this lab is to complete this implementation according to the specifications given in the Canvas
assignment. You are free to write all the code in the single `GameServer.java` file or create additional files
as needed. However, the `test` suite will instantiate your `GameServer` object in a very specific way, so your
implementation must respect this (and you cannot change the `test` suite). You are welcome (and encouraged) to
read the `test` code to see how the tests work and what they are testing.


### Understanding the Test Suite

The `test` package includes two sub-packages: `test.util` includes the generic testing framework and `test.gameServer`
includes the specific test code that is run on the `GameServer` object. We'll continue to use the `test.util`
framework in future labs, so it's probably worth understanding at a high level.

Each specific test in the `test.gameServer` package extends the `Test` class from the `test.util` package, and the 
collection of tests is executed and evaluted using the `Series` class from `test.util`. Reading the documentation about
these classes may be useful, especially if you ever want to create your own tests.

There are two sets of tests included in Lab 0, one for the Checkpoint submission and one for the Final submission.  These 
sets of tests are orchestrated by the `test/Lab0CheckpointTests.java` and `test/Lab0FinalTests.java` files, respectively.
There is a third file `test/Lab0Tests.java` that runs all of the Checkpoint and Final tests, provided for your convenience,
as it is not used by our auto-grader.  The individual tests included in each set can be found in the `test/gameServer` 
folder.  For each test, an instance of the `GameServer` object is created using a Java `ProcessBuilder` to spawn the 
`GameServer` as though created at the command line using the command
```
java gameServer.GameServer <port>
```
where `<port>` is the desired port number where the server will listen on the local machine (i.e., at address 
`localhost:<port>`).  This means that your `GameServer.java` file must include a `main` function that takes one
command-line argument.  All subsequent interaction with the `GameServer` object is through sockets, and the test
code will expect the server to be present and responsive on the given socket.


### Testing the GameServer and running the tests

Once you're at the point where you want to run any of the provided tests, you can use the provided `make` rules. To run
the set of Checkpoint tests, execute `make checkpoint` from the main working directory of the lab. Similarly, to run the 
Final tests, execute `make final`. If you want to run all of the tests (checkpoint and final), you can execute `make all`.
You can also run subsets of tests by commenting out test Classes in the Lab 0 test files.

Before you're ready to run the tests, it's probably helpful to interact with your `GameServer` manually, where you are 
taking the roles of the game players. By far the easiest way to do this is to use the netcat (`nc`) utility, but there are
many options available to you.  Using netcat, all you need to do is start your game server in one terminal and connect to
it as players in other terminals.  For example, if I want to run a game server on port 14736 and connect multiple players, 
I would execute
```
java gameServer.GameServer 14736
```
in one terminal and execute
```
nc localhost 14736
```
in as many other terminals as I want to have game players. Both of these commands initially hang with an empty terminal, and
each player simply enters their text game commands into the terminal, pressing Enter/Return after each.  For example, if I enter
the command `HELLO playerOne`, I'm introducing myself as a player named `playerOne`, and I will see the corresponding response
`Welcome to Word Count playerOne! Do you want to create a new game or join an existing game?` which was received from the game
server. If you want your game server to print anything to its terminal (which is very helpful), you'll need to program it to do
so.  However, as your game progresses, it might make sense to log server interactions to a file instead of the terminal, as 
there's really a lot of stuff going on in the fully working game.

If you're curious to see more details of the socket communication between your game server and your players, you can use network
utilities like `wireshark`/`tshark`.

You are welcome to create additional `make` rules in the Makefile, but we ask that you keep the existing `test` and `checkpoint`
rules, as we will use them for lab grading.


### Generating documentation

You may also notice that there is a significant amount of useful documentation in the form of comments for many of the packages,
files, classes, variables, etc. in the test suite. Rather than digging through all of the files to read the comments, it may be 
helpful to create a nice browseable version of the documentation using the Javadocs utility that is built into the JDK. Our Makefile
already includes some useful rules to create this for you.  If you execute `make docs-test` from the main project directory, the 
`javadoc` utility will create a folder called `doc-test` that includes (among many other things) an `index.html` file where you 
can navigate through a nicely formatted package specification.

We also want you to get in the habit of documenting your code in a way that leads to detailed, easy-to-read/-navigate package 
documentation for your `gameServer` package. Our Makefile includes a `docs` rule that will create a separate folder called `doc`
with a similarly browseable spec. This is the output that we will use for the manually graded parts of the lab, so good comments
are valuable.


### Questions?

If there is any part of the initial repository, environment setup, lab requirements, or anything else, please do not hesitate
to ask.  We're here to help!
