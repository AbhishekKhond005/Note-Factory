FROM node:20-alpine

# Create a non-root user for running opencode
RUN addgroup -S opencode && adduser -S opencode -G opencode

# Install opencode globally
RUN npm install -g opencode@latest

# Set up working directory
WORKDIR /work
RUN chown -R opencode:opencode /work

# Switch to non-root user
USER opencode

# Ensure local bin is in PATH just in case
ENV PATH="/home/opencode/.npm-global/bin:${PATH}"

# Keep image ready
ENTRYPOINT ["opencode"]
