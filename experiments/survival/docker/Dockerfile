FROM eclipse-temurin:11-jdk-jammy
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
        maven git curl ca-certificates unzip python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*
RUN pip3 install --no-cache-dir tiktoken javalang requests
ENV _JAVA_OPTIONS="-Duser.language=en -Duser.country=US -Duser.timezone=UTC -Dfile.encoding=UTF-8"
ENV MAVEN_OPTS="-Duser.language=en -Duser.country=US -Duser.timezone=UTC -Dfile.encoding=UTF-8"
ENV PYTHONUNBUFFERED=1 TZ=UTC LANG=C.UTF-8
WORKDIR /work/experiments/survival
CMD ["bash"]
