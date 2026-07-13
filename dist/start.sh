#!/bin/bash
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
java \
  --module-path /usr/share/openjfx/lib \
  --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.graphics,javafx.base \
  -cp "$SCRIPT_DIR/lib/gson-2.10.1.jar:$SCRIPT_DIR/yinyue-player.jar" \
  -Djava.library.path=/usr/lib/x86_64-linux-gnu/jni \
  com.yinyue.player.Main