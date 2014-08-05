# A simple make file that automates many common tasks when building the GrAF
# tools for GATE

## Where the plugins will be installed.
GRAF=$(PLUGINS)/GrAF

help:
	echo "Available goals:"
	@echo ""
	@echo "  all     : cleans and compiles the jar file."
	@echo "  jar     : compiles the jar file (mvn package)."
	@echo "  clean   : cleans build artifacts."
	@echo "  skip    : builds the jar skipping unit tests."
	@echo "  install : copys the jar to Gate's plugins directory."
	@echo "  upload  : uploads distributions to the ANC server."
	@echo "  test    : used for debugging the Makefile."
	@echo ""

all:
	mvn clean package

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
	mv GrAF $(PLUGINS)
	#mv GrAF-GATE-$(version).jar $(PLUGINS)

upload:
	$(eval version := $(shell cat VERSION))
	echo "Uploading GrAF GATE Tools version "$(version)
	if [ -e target/GrAF-dist-$(version).tar.gz ] ; then \
		mv target/GrAF-dist-$(version).tar.gz target/GrAF-GATE-$(version).tar.gz ; \
		scp -P 22022 target/GrAF-GATE-$(version).tar.gz suderman@anc.org:/home/www/anc/tools ; \
	fi
	if [ -e target/GrAF-dist-$(version).zip ] ; then \
		mv target/GrAF-dist-$(version).zip target/GrAF-GATE-$(version).zip ; \
		scp -P 22022 target/GrAF-GATE-$(version).zip suderman@anc.org:/home/www/anc/tools ; \
		scp -P 22022 target/GrAF-GATE-$(version).zip suderman@anc.org:/home/www/anc/tools/gate ; \
		#unzip target/GrAF-GATE-$(version).zip -d target ; \
		#mv target/GrAF target/GrAF-GATE-$(version) ; \
		#scp -r -P 22022 target/GrAF-GATE-$(version) suderman@anc.org:/home/www/anc/tools/gate ; \
	fi
	
test:
	echo "PLUGINS "$(PLUGINS)
	