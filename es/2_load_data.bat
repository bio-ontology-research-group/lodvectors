
curl -XDELETE http://localhost:9200/vector/ 
curl -XPUT http://localhost:9200/vector/ --data-binary @settings.json
curl -XPUT http://localhost:9200/vector/_mapping/vector --data-binary @mappings.json

split -l 100000 uniprot.json
curl -H"Content-type: application/json" -H"Expect:" http://localhost:9200/vector/_bulk --data-binary @xaa
curl -H"Content-type: application/json" -H"Expect:" http://localhost:9200/vector/_bulk --data-binary @xab
curl -H"Content-type: application/json" -H"Expect:" http://localhost:9200/vector/_bulk --data-binary @xac
curl -H"Content-type: application/json" -H"Expect:" http://localhost:9200/vector/_bulk --data-binary @xad
curl -H"Content-type: application/json" -H"Expect:" http://localhost:9200/vector/_bulk --data-binary @xae
