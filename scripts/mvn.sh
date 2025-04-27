#!/bin/bash

execute_maven()
{
		cd "$(dirname "$0")/.."

	case $1 in
		"compile")
			mvn -s settings.xml compile
			;;

		"test")
			mvn -s settings.xml test
			;;

		"package")
			mvn -s settings.xml package
			;;

		"javadoc")
			mvn -s settings.xml javadoc:javadoc
			;;

		"clean")
			mvn -s settings.xml clean
			;;

		*)
			echo "Invalid option: $1"
			echo "Available options: compile, test, package, javadoc, clean"
			exit 1
			;;
	esac
}

main()
{
	if [ $# -ne 1 ]; then
		echo "Usage: $0 [compile|test|package|javadoc|clean]"
		exit 1
	fi

	execute_maven "$1"
}

main "$@"
