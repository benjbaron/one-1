#! /bin/sh
java -Xmx512M -cp .:lib/ECLA.jar:lib/DTNConsoleConnection.jar:lib/jackson-core-2.5.0.jar:lib/jackson-databind-2.5.0.jar:lib/jackson-annotations-2.5.0.jar:lib/javassist-3.18.2-GA.jar:lib/cglib-nodep-2.2.2.jar:lib/objenesis-2.1.jar core.DTNSim $*
