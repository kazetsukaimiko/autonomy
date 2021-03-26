#!/bin/bash
#

systemctl --user daemon-reload
systemctl --user start autonomy
journalctl -n1000 -f --user-unit autonomy.service
