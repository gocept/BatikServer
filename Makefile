## Makefile for BatikServer


#### customise the following section
JAVA_HOME=/usr
JAVAC=$(JAVA_HOME)/bin/javac
WGET=wget
UNZIP=unzip
TAR=tar
RM=rm
MKDIR=mkdir
CP=cp
MV=mv
CAT=cat
CHMOD=chmod
BASENAME=basename
SED=sed

INSTALL_DIR=./BatikServer
#### end of customisation-section

BATIK_ZIP=batik-1.6.zip
LIB_DIR=$(INSTALL_DIR)/lib
TMP_DIR=./tmp
RAS=$(TMP_DIR)/batik/batik-rasterizer.jar
SRC_LIB=./src/lib
VERSION=$(shell $(CAT) version.txt)
LINUX_DIR=./BatikServer_$(VERSION)_Linux
WINDOWS_DIR=./BatikServer_$(VERSION)_Windows

all:
	@echo "use make install, make linux or make windows"

install: install_create
linux: linux_create
	tar czf $(LINUX_DIR).tgz $(LINUX_DIR)

windows: windows_create
	$(MKDIR) -p -m 755 $(WINDOWS_DIR)/setup
	$(CP) src/setup/*.txt $(WINDOWS_DIR)/setup
	$(SED) "s/%version%/$(VERSION)/" src/setup/setup.iss.in > $(WINDOWS_DIR)/setup.iss
	$(SED) "s/%version%/$(VERSION)/" src/setup/makeWindowsVersion.sh.in > $(WINDOWS_DIR)/makeWindowsVersion.sh
	$(TAR) czf $(WINDOWS_DIR).tgz $(WINDOWS_DIR)

$(SRC_LIB)/*.class: $(SRC_LIB)/*.java
	$(JAVAC) -deprecation -classpath $(RAS) $<

# template for creation
# Parameters: 1 ... version (install, linux or windows)
#             2 ... src directory (linux or windows)
#             3 ... target directory
define PROGRAM_template
 $(1)_create: $$(RAS) $$(SRC_LIB)/*.class
	$$(MKDIR) -p -m 755 $(3)
	$$(MKDIR) -p -m 755 $(3)/bin
	$$(CP) src/bin/$(2)/batikServer* $(3)/bin
	$$(CP) src/bin/$(2)/wrapper* $(3)/bin
	if [ $(2) = "windows" ] ; then \
		$$(CP) src/bin/$(2)/InstallBatikServer-NT.bat $(3)/bin ; \
		$$(CP) src/bin/$(2)/UninstallBatikServer-NT.bat $(3)/bin ; \
	fi
	$$(CHMOD) 755 $(3)/bin/*
	$$(MKDIR) -p -m 755 $(3)/conf
	$$(CP) src/conf/$(2)/batikServer.config $(3)/conf
	$$(CP) src/conf/$(2)/wrapper.conf $(3)/conf
	$$(MKDIR) -p -m 755 $(3)/lib
	if [ $(2) = "windows" ] ; then \
		$$(CP) $$(SRC_LIB)/$(2)/wrapper.dll $(3)/lib ; \
	else \
		$$(CP) $$(SRC_LIB)/$(2)/libwrapper.so $(3)/lib ; \
	fi
	$$(CP) $$(SRC_LIB)/$(2)/wrapper.jar $(3)/lib
	$$(CP) $$(SRC_LIB)/batikServer.class $(3)/lib
	$$(CP) -r $(TMP_DIR)/batik $(3)/lib
	$$(MKDIR) -p -m 755 $(3)/logs
	$$(MKDIR) -p -m 755 $(3)/doc
	$$(CP) doc/*.txt $(3)/doc
	$$(MV) $(3)/doc/README.txt $(3)
	$$(CP) $(TMP_DIR)/batik/LICENSE $(3)/doc/LICENSE_batik.txt
	$$(MKDIR) -p -m 755 $(3)/doc/german
	$$(CP) doc/german/*.txt $(3)/doc/german
endef

# create targets for install_create, linux_create, windows_create
$(eval $(call PROGRAM_template,install,linux,$(INSTALL_DIR)))
$(eval $(call PROGRAM_template,linux,linux,$(LINUX_DIR)))
$(eval $(call PROGRAM_template,windows,windows,$(WINDOWS_DIR)))


clean:
	$(RM) -f $(SRC_LIB)/*.class
	$(RM) -rf $(TMP_DIR)
	$(RM) -rf ./BatikServer
	$(RM) -rf $(LINUX_DIR)
	$(RM) -rf $(LINUX_DIR).tgz
	$(RM) -rf $(WINDOWS_DIR)
	$(RM) -rf $(WINDOWS_DIR).tgz


$(RAS): $(TMP_DIR)/$(BATIK_ZIP)

$(TMP_DIR)/$(BATIK_ZIP):
	$(MKDIR) -p -m 755 $(TMP_DIR)
	cd $(TMP_DIR); \
	$(WGET) http://archive.apache.org/dist/xml/batik/$(BATIK_ZIP) ; \
	$(UNZIP) -qq $(BATIK_ZIP) ; \
	$(MV) `$(BASENAME) $(BATIK_ZIP) .zip` batik
	$(RM) -rf $(TMP_DIR)/batik/samples
	$(RM) -rf $(TMP_DIR)/batik/docs
	$(RM) -f $(TMP_DIR)/batik/batik-squiggle.jar
