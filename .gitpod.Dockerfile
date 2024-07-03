FROM gitpod/workspace-full

SHELL ["/bin/bash", "-c"]
RUN sudo apt update && sudo apt upgrade -y && sudo apt autoremove
RUN source "/home/gitpod/.sdkman/bin/sdkman-init.sh"  \
    && sdk install java 21.0.2-zulu < /dev/null && sdk install gradle < /dev/null

RUN source /etc/lsb-release
