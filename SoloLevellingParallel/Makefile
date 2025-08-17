JAVAC=javac
JAVA=java
SRC=.
CLASSES = $(SRC)/DungeonMapParallel.java $(SRC)/HuntParallel.java $(SRC)/DungeonHunterParallel.java
# Default arguments (update these if needed)
ARGS ?= 20 0.2 0  # Replace 'default_arguments' with your specific default arguments, if any

all:
	$(JAVAC) $(CLASSES)

run:
	$(JAVA) -cp $(SRC) DungeonHunterParallel $(ARGS) 

run-default-and-custom:
	$(MAKE) run
	$(MAKE) run ARGS="100 0.2 0"

clean:
	rm -f $(SRC)/*.class
