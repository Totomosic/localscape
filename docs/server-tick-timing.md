# WSL clock skew and slow server ticks

Diagnosed on 2026-10-17 with RSMod running in Ubuntu 24.04 on WSL 2 and
RuneLite connecting through Windows RSProx.

Tick Tracker reported roughly 630 ms ticks, and an external gameplay timer also
showed slow ticks. The server's deadline scheduler nevertheless measured about
600 ms, because it used the same slow operating-system clock as the measurement.

## Evidence

- The live game thread averaged 599.99 ms between waits in a JVM recording.
- Windows RSProx recorded incoming tick bursts roughly 635–667 ms apart and
  forwarded them immediately. The delay already existed before proxy forwarding.
- Over one Windows stopwatch sample of 15.232 seconds, WSL's adjusted monotonic
  clock advanced only 14.228 seconds. Its raw monotonic clock advanced 15.224
  seconds, agreeing with Windows.
- Linux's kernel `adjtimex` tick setting was around 9,340–9,400 microseconds,
  compared with the normal 10,000. A nominal 600 ms server tick therefore took
  roughly 640 ms of real time.
- Ubuntu's `systemd-timesyncd` and WSL's host clock synchronizer were both
  adjusting the shared system clock. A temporary kernel trace identified
  `chronyd` in WSL's system environment applying the remaining corrections after
  Ubuntu's NTP service stopped.

## Fix applied

With explicit permission to change WSL's time configuration, disabled and stopped
`systemd-timesyncd.service`, then restored the normal kernel tick and cleared the
frequency adjustment. WSL's host synchronizer remains active; it needed time to
settle after the competing adjustments stopped. The kernel tick then remained
at 10,000 microseconds with only a small normal frequency correction.

This follows [Ubuntu's WSL time synchronization guidance](https://ubuntu.com/wsl/docs/stable/explanation/time-sync/),
which recommends disabling `systemd-timesyncd` on Ubuntu 24.04 when Hyper-V
implicit synchronization is enabled. That setting was already enabled here.

After settling, Windows elapsed time was 15.231 seconds and WSL's adjusted and
raw monotonic clocks both advanced 15.223 seconds. The small difference includes
the overhead of delivering the first stopwatch sample between processes.

The final Windows socket recording contained no events because the client had
disconnected. Clock-rate recovery was verified independently, and the user
subsequently confirmed that the in-game timing was fixed after reconnecting.

The service disable persists across WSL restarts. No server or client restart was
needed to apply the live correction. Tick Tracker retains a session average;
logging out and back in starts a fresh measurement.

## If this recurs

Compare WSL's `CLOCK_MONOTONIC` with `CLOCK_MONOTONIC_RAW` over the same interval,
and measure tick arrivals on Windows. Measuring the scheduler only against its
own clock can hide this fault. Check for competing time synchronization services
before changing the server's configured 600 ms tick interval.
