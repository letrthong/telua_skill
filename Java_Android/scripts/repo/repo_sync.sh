#!/bin/bash
# ==============================================================================
# Copyright (C) 2026 letrthong@gmail.com
# Created & Maintained by: letrthong@gmail.com
# Generated & Refactored by: Gemini 3.6 Pro (Google DeepMind)
# Licensed under the Apache License, Version 2.0
# ==============================================================================

repo sync -d -c --force-sync --no-clone-bundle --tags -j4 && ./repo-lfs
