docker run --name test-enclave -it -d -v "C:/Users/Victor Stoian/IdeaProjects/TestConclave:/test" -w /test ubuntu bash
docker exec -ti test-enclave apt update
docker exec -ti test-enclave apt install -y openjdk-8-jdk
docker exec -ti test-enclave tar xf /test/host/build/distributions/host.tar -C /tmp/
docker exec -ti test-enclave /tmp/host/bin/host