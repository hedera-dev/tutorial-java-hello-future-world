FROM gitpod/workspace-full:2024-09-11-00-04-27

USER root
RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && \
    sdk install java 22.0.2-tem && \
    sdk default java 22.0.2-tem"
