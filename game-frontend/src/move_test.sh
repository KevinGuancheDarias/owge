find ./app -iname '*.spec.ts' \
	-exec bash -c 'export myPath="./test/`dirname {} | cut -d / -f 3-`"; (test ! -d "$myPath" && echo "$myPath does not exists, creating" && mkdir "$myPath" ); (test -d "$myPath" && echo "Moving `basename {}` to $myPath" && mv {} "$myPath/" ) ' \;
