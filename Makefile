# Build without NetBeans. Requires a JDK (17+) on PATH.
# The MySQL Connector/J jar is only needed at runtime: make run MYSQL_JAR=path/to/mysql-connector-j.jar

SRC_DIR   := src
TEST_DIR  := test
OUT_DIR   := out
MAIN      := studentmanagementsystem.StudentManagementSystem
MYSQL_JAR ?= lib/mysql-connector-j.jar

SOURCES      := $(shell find $(SRC_DIR) -name '*.java')
TEST_SOURCES := $(shell find $(TEST_DIR) -name '*.java')

.PHONY: all build test run clean

all: build

build:
	mkdir -p $(OUT_DIR)/classes
	javac -d $(OUT_DIR)/classes $(SOURCES)

test: build
	mkdir -p $(OUT_DIR)/test-classes
	javac -cp $(OUT_DIR)/classes -d $(OUT_DIR)/test-classes $(TEST_SOURCES)
	java -cp $(OUT_DIR)/classes:$(OUT_DIR)/test-classes studentmanagementsystem.ValidationTest

run: build
	java -cp $(OUT_DIR)/classes:$(MYSQL_JAR) $(MAIN)

clean:
	rm -rf $(OUT_DIR)
