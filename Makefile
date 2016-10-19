# The default target is to build all the binaries.
all:

-include Makefile.local

# Some variables need to be set in order to build any of the code
ifeq ($(FIRRTL_HOME),)
$(error FIRRTL_HOME must be set, it should probably point to rocket-chip/firrtl)
endif

ifeq ($(CMD_SBT),)
$(error CMD_SBT must be set, it should point to an SBT run script)
endif

# The clean target deletes everything that's been created
.PHONY: clean
clean::
	rm -rf bin obj check

# Builds wrapper executables for the various FIRRTL passes
all: bin/GenerateTop
all: bin/GenerateHarness

bin/%: tools/generate-sbt-wrapper-script obj/%/stamp
	@mkdir -p $(dir $@)
	$< --sbt $(abspath $(CMD_SBT)) --basedir $(abspath obj/$(notdir $@)) --main $(notdir $@) --output $@

obj/%/stamp: $(shell find src -iname "*.scala")
	@mkdir -p $(dir $@)
	rsync -rv --delete $(FIRRTL_HOME) $(dir $@)
	cp -a src/firrtl/* $(dir $@)
	sed 's/@@TOP_NAME_LOWER@@/$(shell echo $(notdir $@) | tr A-Z a-z)/g' -i $(dir $@)/project/build.scala
	sed 's/@@TOP_NAME_UPPER@@/$(shell echo $(notdir $@) | tr A-Z A-Z)/g' -i $(dir $@)/project/build.scala
	date > $@

# Test cases
.PHONY: check
check: $(patsubst test/%,check/%,$(shell find test -type f -iname "*.bash"))

check/GenerateHarness/%: test/GenerateHarness/% bin/GenerateHarness
	@mkdir -p $(dir $@)
	ptest --test $(abspath $<) --out $@ --args $(abspath $^)

# This prints the status of various test cases
.PHONY: report
report: check
	@+ptest
