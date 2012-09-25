mkdir -p classes
javac -d classes -classpath classes/:lib/jackson-all-1.8.4.jar:lib/netty-4.0.0.Alpha4.jar src/*/* 
