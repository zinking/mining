#!/bin/bash
cd /mining-solution/mining/
activator publish-local > /mining-solution/mining-play/log/application.log 2>&1
cd /mining-solution/mining-play/
activator run > /mining-solution/mining-play/log/application.log 2>&1