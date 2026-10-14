#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

# This command runs in the proxy window. Wait for the HTTP window's service before
# launching RSProx, since ./dev proxy checks its configuration endpoints first.
if [[ "${1:-}" == "--proxy-window" ]]; then
    cd -- "$project_root"
    python3 - <<'PY'
import json
import time
import urllib.error
import urllib.request

with open("project.json") as source:
    port = json.load(source)["http_port"]
url = f"http://127.0.0.1:{port}/bootstrap.json"
print("Waiting for the local HTTP service…", flush=True)
deadline = time.monotonic() + 60
while time.monotonic() < deadline:
    try:
        with urllib.request.urlopen(url, timeout=1) as response:
            response.read()
        break
    except (OSError, urllib.error.URLError):
        time.sleep(1)
else:
    raise SystemExit("HTTP service did not start. Check the HTTP window, then rerun ./dev proxy.")
PY
    exec ./dev proxy
fi

session="localscape"
detach=false
while (($#)); do
    case "$1" in
        --detach) detach=true ;;
        -h|--help)
            printf 'Usage: %s [--detach] [session-name]\n' "$0"
            exit 0
            ;;
        -*) printf 'Unknown option: %s\n' "$1" >&2; exit 1 ;;
        *) session="$1" ;;
    esac
    shift
done

if [[ ! "$session" =~ ^[a-zA-Z0-9_-]+$ ]]; then
    printf 'Session name must contain only letters, numbers, underscores or hyphens.\n' >&2
    exit 1
fi
if ! command -v tmux >/dev/null 2>&1; then
    printf 'tmux is required. Install it, then rerun this script.\n' >&2
    exit 1
fi

if ! tmux has-session -t "=$session" 2>/dev/null; then
    http_pane="$(tmux new-session -d -s "$session" -n http -c "$project_root" -P -F '#{pane_id}')"
    server_pane="$(tmux new-window -d -t "=$session" -n rsmod -c "$project_root" -P -F '#{pane_id}')"
    proxy_pane="$(tmux new-window -d -t "=$session" -n rsprox -c "$project_root" -P -F '#{pane_id}')"
    shell_window="$(tmux new-window -d -t "=$session" -n shell -c "$project_root" -P -F '#{window_id}')"
    tmux set-option -w -t "$http_pane" automatic-rename off
    tmux set-option -w -t "$server_pane" automatic-rename off
    tmux set-option -w -t "$proxy_pane" automatic-rename off
    tmux set-option -w -t "$shell_window" automatic-rename off
    tmux send-keys -t "$http_pane" -l './dev serve'
    tmux send-keys -t "$http_pane" Enter
    tmux send-keys -t "$server_pane" -l './dev server'
    tmux send-keys -t "$server_pane" Enter
    tmux send-keys -t "$proxy_pane" -l './tools/tmux.sh --proxy-window'
    tmux send-keys -t "$proxy_pane" Enter
    tmux select-window -t "$shell_window"
fi

if "$detach"; then
    printf 'Session ready. Attach with: tmux attach-session -t %s\n' "$session"
elif [[ -n "${TMUX:-}" ]]; then
    exec tmux switch-client -t "=$session"
else
    exec tmux attach-session -t "=$session"
fi
