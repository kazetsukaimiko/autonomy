#!/bin/bash
#
systemctl --user daemon-reload
journalctl -n1000 -f --user-unit autonomy.service
