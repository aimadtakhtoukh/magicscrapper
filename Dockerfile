FROM williamyeh/scala:2.12.6
VOLUME /Gatherer/Images
WORKDIR /
ADD target/scala-2.12/magicscrapper-assembly-0.1.jar app.jar

CMD scala app.jar