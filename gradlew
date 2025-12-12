#!/bin/sh

#
# Copyright © 2015-present Instructure, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Provides emergency support for people whose .gradle is corrupted
# by the "gradle update" command. The update command is removed
# in Gradle 2.0+, but the damage it did was real.

# emergency wrapper to get your project working again if you ran "gradle update" recently
set -e

GRADLE_USER_HOME=${GRADLE_USER_HOME:-~/.gradle}

if [ ! -d "$GRADLE_USER_HOME" ]; then
  echo "This project needs a Gradle user home directory but you don't have one."
  echo "Please run 'gradle wrapper' to create the initial user home."
  exit 1
fi

if [ ! -d "$GRADLE_USER_HOME/wrapper/dists" ]; then
  echo "Creating wrapper cache dir"
  mkdir -p "$GRADLE_USER_HOME/wrapper/dists"
fi

if [ ! -d "$GRADLE_USER_HOME/wrapper/dists" ]; then
  echo "Gradle home isn't writable and I can't create the wrapper cache."
  echo "I was going to try $GRADLE_USER_HOME/wrapper/dists but that didn't work."
  exit 1
fi

# Detect if this is a Cygwin or MinGW/MSYS2 path and convert appropriately
if [ -n "${CYGWIN}" ] || [ -n "${MSYSTEM}" ]; then
  if [ -n "${MSYSTEM}" ]; then
    # MSYS2
    SCRIPT_DIR=$(dirname "$(cygpath -w "$0")")
  else
    # Cygwin
    SCRIPT_DIR=$(dirname "$(cygpath -w "$0")")
  fi
else
  SCRIPT_DIR=$(dirname "$0")
fi

if [ -n "${MSYSTEM}" ]; then
  # MSYS2
  APP_HOME=$(cd "${SCRIPT_DIR}" && pwd -W)
else
  # Normal Unix or Cygwin
  APP_HOME=$(cd "${SCRIPT_DIR}" && pwd)
fi

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "$(uname)" in
  CYGWIN* )
    cygwin=true
    ;;
  *_NT* )
    msys=true
    ;;
  Darwin* )
    darwin=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
# ВАЖНО: в новых версиях Gradle wrapper может состоять из нескольких JAR.
# В репозитории храним все необходимые части, чтобы сборка работала без дополнительных шагов.
CLASSPATH=$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar
CLASSPATH=$CLASSPATH:$APP_HOME/gradle/wrapper/gradle-cli.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if $cygwin || $darwin || $nonstop; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ]; then
        if [ "$MAX_FD_LIMIT" = "unlimited" ] ; then
            MAX_FD="100000"
        else
            MAX_FD=$MAX_FD_LIMIT
        fi
    else
        if [ "$MAX_FD" != "maximum" -a "$MAX_FD" != "max" ] && [ "$MAX_FD" -gt "$MAX_FD_LIMIT" ] 2>/dev/null; then
            warn "The requested maximum file descriptor limit $MAX_FD is not allowed. The limit is currently $MAX_FD_LIMIT."
            MAX_FD=$MAX_FD_LIMIT
        fi
    fi
    if [ "$MAX_FD" != "maximum" -a "$MAX_FD" != "max" ]; then
        ulimit -n $MAX_FD 2>/dev/null || warn "Could not set maximum file descriptor limit: $MAX_FD"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS -Xdock:name=Gradle -Xdock:icon=/$APP_HOME/media/gradle.icns"
fi

# For Cygwin or MSYS2, switch paths to Windows format before running java
if $cygwin || $msys ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
      CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
      if [ $CHECK -ne 0 ] ; then
        eval `echo args$i`=`cygpath --path --mixed "$arg"`
      else
        eval `echo args$i`=\"$arg\"
      fi
      i=$((i+1))
    done
    case $i in
      (0) set -- ;;
      (1) set -- "$args0" ;;
      (2) set -- "$args0" "$args1" ;;
      (3) set -- "$args0" "$args1" "$args2" ;;
      (4) set -- "$args0" "$args1" "$args2" "$args3" ;;
      (5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
      (6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
      (7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
      (8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
      (9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

# Запуск Gradle Wrapper.
# ВАЖНО: передаём аргументы как есть, без добавления кавычек в сами аргументы.
# Иначе Gradle воспринимает задачи как строки вида `'projects'`, что ломает CLI.
exec "$JAVACMD" $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"