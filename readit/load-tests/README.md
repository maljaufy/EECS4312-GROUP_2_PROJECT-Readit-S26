# Voting endpoint load test

`voting-endpoint.jmx` drives the JSON endpoint implemented at `POST /api/votes`.
It defaults to 100 concurrent users and alternates upvotes/downvotes so each
iteration exercises persistence rather than only the no-op idempotency path.

Prepare a post whose ID is `1` and at least 100 non-author users starting at ID
`2`, start the application and infrastructure, then run:

```shell
jmeter -n -t load-tests/voting-endpoint.jmx -l target/voting-load.jtl \
  -Jhost=localhost -Jport=8080 -Jusers=100 -Jiterations=20 \
  -Jvoter_start=2 -Jtarget_id=1
```

The post author must not be inside the configured voter ID range. JMeter exits
non-zero when the response assertion sees a status other than HTTP 200.
