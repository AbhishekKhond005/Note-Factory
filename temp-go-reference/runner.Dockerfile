FROM alpine:3.21

# Install dependencies
RUN apk add --no-cache curl bash ca-certificates git

# Install opencode via official install script
RUN curl -fsSL https://opencode.ai/install | bash

# Create a non-root user
RUN addgroup -S opencode && adduser -S opencode -G opencode

# Set up working directory
WORKDIR /work
RUN chown -R opencode:opencode /work

# Make sure opencode is in PATH
ENV PATH="/root/.opencode/bin:/home/opencode/.opencode/bin:${PATH}"

# Switch to non-root user
USER opencode

# Keep image ready
ENTRYPOINT ["opencode"]
