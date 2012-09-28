# A simple make file that automates many common tasks when building Grate.

## CHANGE. This is the startup script that will be generated. This path can
## point anywhere, but should be on your (the user's) default $PATH.
PLUGINS=$(HOME)/Documents/GatePlugins
GRAF=$(PLUGINS)/GrAF

jar:
	mvn clean package
	
clean:
	mvn clean
	
skip:
	mvn -DskipTests=true package
	
install:
	$(eval version := $(shell cat VERSION))
	echo "Copying GrAF Tools version "$(version)
	tar -xzf target/GrAF-dist-$(version).tar.gz
	if [ -d $(GRAF) ] ; then rm -rf $(GRAF) ; fi
	mv GrAF $(PLUGINS)

