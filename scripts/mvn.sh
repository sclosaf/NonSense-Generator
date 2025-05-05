#!/bin/bash

execute_maven()
{
		cd "$(dirname "$0")/.."

	case $1 in
		"compile")
			mvn -s config/settings.xml compile
			;;

		"test")
			mvn -s config/settings.xml test
			;;

		"package")
			mvn -s config/settings.xml package
			;;

		"javadoc")
			mvn -s config/settings.xml javadoc:javadoc
			;;

		"clean")
			mvn -s config/settings.xml clean
			;;

		"execute")
			java -cp "target/nonsense-generator-1.0.jar:target/libs/*:target/config/" unipd.nonsense.App
			;;

		*)
			echo "Invalid option: $1"
			echo "Available options: compile, test, package, javadoc, clean, execute"
			exit 1
			;;
	esac
}

main()
{
	if [ $# -ne 1 ]; then
		echo "Usage: $0 [compile|test|package|javadoc|clean|execute]"
		exit 1
	fi

	execute_maven "$1"
}

main "$@"
