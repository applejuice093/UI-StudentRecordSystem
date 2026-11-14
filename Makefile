# Optional Unix build. NetBeans is the supported way to open and run CoralCove.
# Requires JDK 17+ and make.

SRC_DIR := src
TEST_DIR := test
OUT_DIR := out
MAIN := coralcove.Main
CP := lib/sqlite-jdbc-3.49.1.0.jar:lib/flatlaf-3.6.jar:lib/jfreechart-1.5.6.jar
TEST_CP := $(CP):lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar

SOURCES := $(shell find $(SRC_DIR) -name '*.java')
TEST_SOURCES := $(shell find $(TEST_DIR) -name '*.java')

.PHONY: all build test run clean

all: build

build:
	rm -rf $(OUT_DIR)/classes
	mkdir -p $(OUT_DIR)/classes
	javac --release 17 -encoding UTF-8 -cp $(CP) -d $(OUT_DIR)/classes $(SOURCES)

test: build
	mkdir -p $(OUT_DIR)/test-classes
	javac --release 17 -encoding UTF-8 -cp $(OUT_DIR)/classes:$(TEST_CP) -d $(OUT_DIR)/test-classes $(TEST_SOURCES)
	java -cp $(OUT_DIR)/classes:$(OUT_DIR)/test-classes:$(TEST_CP) org.junit.runner.JUnitCore \
		coralcove.PasswordHasherTest coralcove.DatabaseInitializerTest \
		coralcove.AuthenticationServiceTest coralcove.ResortRulesTest coralcove.DemoDataTest

run: build
	java -cp $(OUT_DIR)/classes:$(CP) $(MAIN)

clean:
	rm -rf $(OUT_DIR)
