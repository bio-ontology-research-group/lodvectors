<?php
# get files from http://lod4ml.org/

if(!isset($argv[1])) {
	echo "specify the dataset name from http://lod4ml.org/ to process";
	exit;
}

$in = $argv[1];
$map = $in.".map";
$vec = $in.".vec";
$out = $in.".json";

if(!file_exists($map)) {
	echo $map." does not exist";
	exit;
}
$fp = fopen($map,"r");
while($l = fgets($fp))
{
	$a = explode("\t",$l);
	$ids[trim($a[1])] = trim($a[0]);
}
fclose($fp);

if($in == "uniprot") {
	$file = "proteins.csv";
	$fp = fopen($file,"r");
	fgetcsv($fp); // header
	while($a = fgetcsv($fp)) {
		$labels[ $a[0] ] = $a[1];
	}
}


$count = count($ids);
echo $count.PHP_EOL;

$fp = fopen($vec,"r");
$fout = fopen($out,"w");
while($l = fgets($fp,20000))
{
	$a = explode(" ",$l);
	$id = $a[0];
	$label = '';
	$uri = substr($ids[$id],1,-1);
	if(strstr($uri,"http://purl.uniprot.org/uniprot/")) {
		$id = "uniprot.".substr($uri,strlen("http://purl.uniprot.org/uniprot/"));
		if(strstr($uri,"#")) {
			continue;
		}
		if(isset($labels[$uri])) $label = '"label":"'.$labels[ $uri ].'",';
	}

	array_shift($a);
	$buf = '';
	foreach($a as $k => $v) {$buf .= "$k|$v ";}
	$buf = trim($buf);
	
	$e =  '{"index":{"_index":"vector","_type":"'.$in.'","_id":"'.$id.'"}}'.PHP_EOL;
	$e .= '{'.$label.'"name": "'.$uri.'","@model_factor":"'.$buf.'"}'.PHP_EOL;
	fwrite($fout,$e);
}
fclose($fp);
fclose($fout);
