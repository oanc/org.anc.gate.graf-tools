# A simple make file that automates many common tasks when building Grate.

## CHANGE. This is the startup script that will be generated. This path can
## point anywhere, but should be on your (the user's) default $PATH.
GRAF=$(PLUGINS)/GrAF

jar:
	mvn package
	
clean:
	mvn clean
	
skip:
	mvn -DskipTests=true package
	
install:
	$(eval version := $(shell cat VERSION))
	echo "Copying GrAF Tools version "$(version)
	tar -xzf target/GrAF-dist-$(version).tar.gz
	if [ -d $(GRAF) ] ; then rm -rf $(GRAF) ; fi
	mv GrAF-GATE-$(version).jar $(PLUGINS)

upload:
	$(eval version := $(shell cat VERSION))
	echo "Uploading GrAF GATE Tools version "$(version)
	if [ -e target/GrAF-dist-$(version).tar.gz ] ; then
		mv target/GrAF-dist-$(version).tar.gz target/GrAF-GATE-$(version).tar.gz
	fi
	if [ -e target/GrAF-dist-$(version).zip ] ; then
		mv target/GrAF-dist-$(version).zip target/GrAF-GATE-$(version).zip
	fi
	scp -P 22022 target/GrAF-GATE-$(version).tar.gz suderman@anc.org:/home/www/anc/tools
	scp -P 22022 target/GrAF-GATE-$(version).zip suderman@anc.org:/home/www/anc/tools