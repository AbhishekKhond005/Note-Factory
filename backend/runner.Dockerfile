FROM ubuntu:latest

# Install dependencies
RUN apt-get update && apt-get install -y curl

# Install opencode globally via official script
RUN curl -fsSL https://opencode.ai/install | bash

# Add opencode to PATH
ENV PATH="/root/.opencode/bin:${PATH}"

WORKDIR /work

ENTRYPOINT ["opencode"]
