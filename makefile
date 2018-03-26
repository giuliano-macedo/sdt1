SRC:=hangman
DEPS:=$(SRC)/*.java
all:server client slave
server:$(SRC)/Server.class
client:$(SRC)/Client.class
slave:$(SRC)/Slave.class
$(SRC)/Server.class:$(DEPS)
	javac $(SRC)/Server.java
$(SRC)/Client.class:$(DEPS)
	javac $(SRC)/Client.java
$(SRC)/Slave.class:$(DEPS)
	javac $(SRC)/Slave.java
.PHONY:runServerTest runClient runSlave clean
runServerTest:$(SRC)/Server.class
	java hangman.Server dicts/part.txt part 1
runClient:$(SRC)/Client.class
	java hangman.Client
runSlave:$(SRC)/Slave.class
	java hangman.Slave
clean:
	rm -rf hangman/*.class
