<?php

if(!isset($argv[1]) && !isset($argv[2])) {
	echo "specify type and name. e.g. uniprot uniprot.P04637".PHP_EOL;
	exit;
}
$type = $argv[1];
$name = $argv[2];

$url = "http://localhost:9200/vector/_search/?q=_id:$name";
$json = file_get_contents($url);
$o = json_decode($json);

if(!isset($o->hits->hits[0])) {
	echo "no results for $name".PHP_EOL;
	exit;
}
$mf = $o->hits->hits[0]->_source->{'@model_factor'};
$a = explode(" ",$mf);
$b = '';
foreach($a as $v)
{
	$c = explode("|",$v);
	$b .= $c[1].",";
}
$vector = "[".substr($b,0,-1)."]";

$q = '
{
    "stored_fields": ["name"],
    "query": {
        "function_score": {
            "query" : {
                "query_string": {
                    "query": "*"
                }
            },
            "script_score": {
                "script": {
                    "inline": "payload_vector_score",
                	"lang": "native",
                	"params": {
                    	"field": "@model_factor",
                    	"vector": '.$vector.',
                    	"cosine" : true
                    }
				}
            },
            "boost_mode": "replace"
        }
    }
}';

file_put_contents("temp.q",$q);
$cmd = "curl http://localhost:9200/_search?pretty -d @temp.q";
$out = system($cmd);









?>