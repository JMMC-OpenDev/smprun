#*******************************************************************************
# JMMC project ( http://www.jmmc.fr ) - Copyright (C) CNRS.
#*******************************************************************************
# REMARKS
#    Makefile based on the VLT/ALMA Makefile
#
#-------------------------------------------------------------------------------

#
# user definable C-compilation flags
#USER_CFLAGS = 

#
# user definable javac compilation flags
#USER_JFLAGS = 

#
# additional include and library search paths
#USER_INC = 
#USER_LIB = 

#
# MODULE CODE DESCRIPTION:
# ------------------------
# As a general rule:  public file are "cleaned" and "installed"  
#                     local (_L) are not "installed".

#
# C programs (public and local)
# -----------------------------
# Next line was needed in one very old architecture
EXECUTABLES     =
EXECUTABLES     =
EXECUTABLES_L   =

#
# <brief description of xxxxx program>
xxxxx_OBJECTS   =
xxxxx_LDFLAGS   =
xxxxx_LIBS      =

#
# special compilation flags for single c sources
#yyyyy_CFLAGS   = 

#
# Includes (.h) files (public only)
# ---------------------------------
INCLUDES        = 

#
# Libraries (public and local)
# ----------------------------
LIBRARIES       = 
LIBRARIES_L     =

#
# <brief description of lllll library>
llllll_OBJECTS   =  			   		\

#
# Scripts (public and local)
# --------------------------
SCRIPTS         =
SCRIPTS_L       =

#
# TCL scripts (public and local)
# ------------------------------
TCL_SCRIPTS     =
TCL_SCRIPTS_L   =

#
# <brief description of tttttt tcl-script>
tttttt_OBJECTS  =
tttttt_TCLSH    = 
tttttt_LIBS     = 

#
# TCL libraries (public and local)
# --------------------------------
TCL_LIBRARIES   =
TCL_LIBRARIES_L =

#
# <brief description of tttlll library>
tttlll_OBJECTS  = 

#
# Python scripts (public and local)
# ---------------------------------
PY_SCRIPTS     =
PY_SCRIPTS_L   =

#
# Jarfiles and their directories
# ---------------------------------
JARFILES       = smprun

#
# <brief description of smprun jar files>
smprun_DIRS     = fr
smprun_EXTRAS   = fr/jmmc/smprun/resource/ApplicationData.xml \
                  fr/jmmc/smprun/resource/*.png

#
# Java programs
# ---------------------------------
JAVA_EXECUTABLES = AppLauncher 

#
# other Jar Files
#
JAVA_JAR = 

#
# <brief description of AppLauncher java program>
AppLauncher_CLASS = fr.jmmc.scalib.smprun.AppLauncher

#
# Command Definition Files (CDF)
# ---------------------------------
CDF_FILES       =

#
# man pages to be done
# --------------------
MANSECTIONS =
MAN1 =
MAN3 =
MAN5 =
MAN7 =
MAN8 =

#
# other files to be installed
#----------------------------
INSTALL_FILES = $(shell ls ../lib/*.jar)

#
# list of all possible C-sources (used to create automatic dependencies)
# ----------------------------------------------------------------------
CSOURCENAMES = \
	$(foreach exe, $(EXECUTABLES) $(EXECUTABLES_L), $($(exe)_OBJECTS)) \
	$(foreach lib, $(LIBRARIES) $(LIBRARIES_L), $($(lib)_OBJECTS))

#
#>>>>> END OF standard rules

#
# INCLUDE STANDARDS
# -----------------
MAKEDIR = $(MCSROOT)/include
include $(MAKEDIR)/mkfMakefile

#
# TARGETS
# -------
all : do_all
	@echo " . . . 'all' done" 

clean : clean_all 
	@echo " . . . clean done"

man : do_man 
	@echo " . . . man page(s) done"

#../lib/smprun-doc.jar:
#	jmcsHTML2HelpSet JMMC-MAN-2600-0001
#	@echo " . . . user documentation done"

HTML_DEST = ~/public_html/stubs/
install_html:
	mkdir -p $(HTML_DEST)
	cp -rf ./public_html/.htaccess $(HTML_DEST)
	cp -rf ./public_html/* $(HTML_DEST)
	cp -f ../config/*.xml $(HTML_DEST)
	cp -f fr/jmmc/smprun/stub/data/SampStubDescriptor.xsd $(HTML_DEST)
	@echo " . . . HTML installation done"

#install : ../lib/smprun-doc.jar install_all
install : install_html install_all
	@echo "......jnlps:"
	jmcsDeployJnlp AppLauncher.jnlp    
	@echo " . . . installation done"


#___oOo___
