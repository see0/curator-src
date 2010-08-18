<?php
$GLOBALS['THRIFT_ROOT'] = 'thrift';
ini_set('error_reporting', E_ALL);
ini_set('display_errors',1);

require_once $GLOBALS['THRIFT_ROOT'].'/Thrift.php';
require_once $GLOBALS['THRIFT_ROOT'].'/protocol/TBinaryProtocol.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TSocket.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/THttpClient.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TBufferedTransport.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TFramedTransport.php';

require_once $GLOBALS['THRIFT_ROOT'].'/packages/labeler/Labeler.php';
require_once $GLOBALS['THRIFT_ROOT'].'/packages/labeler/MultiLabeler.php';
require_once $GLOBALS['THRIFT_ROOT'].'/packages/parser/Parser.php';
require_once $GLOBALS['THRIFT_ROOT'].'/packages/parser/MultiParser.php';
require_once $GLOBALS['THRIFT_ROOT'].'/packages/curator/Curator.php';

require_once 'helperfunctions.php';
require_once 'CURATORCONFIG.php';

define('SMARTY_DIR', 'smarty/');

require_once SMARTY_DIR.'Smarty.class.php';

function perform() {
    $smarty = new Smarty;
    $text = stripslashes($_POST["text"]);
    $store = true;
    $update = isset($_POST['force']) && $_POST['force'] == 'true';
    $ws = isset($_POST['ws']) && $_POST['ws'] == 'true';
    $annotations = $_POST['annotations'];
    
    $host = $_POST['host'];
    $port = $_POST['port'];
    
    $service = $_POST['service'];

    try {
        $hostname = $curator_hostname;
        $port = $curator_port;
            
        $timeout = 300;
        $socket = new TSocket($hostname, $port);
        $socket->setRecvTimeout($timeout*1000);
        $transport = new TBufferedTransport($socket, 1024, 1024);
        $transport = new TFramedTransport($transport);
        $protocol = new TBinaryProtocol($transport);
        $client = new CuratorClient($protocol);
        
        $transport->open();
        if ($ws) {
            $texts = array();
            foreach (explode("\n", $text) as $line) {
                $trline = trim($line);
                if (!empty($trline)) {
                    $texts[] = $line;
                }
            }
            $record = $client->wsgetRecord($texts);
        } else {
            $record = $client->getRecord($text);
        }
        $transport->close();

        foreach ($annotations as $annotation) {
            $transport->open();
            if ($ws) {
                $record = $client->wsprovide($annotation, $texts, $update);
            } else {
                $record = $client->provide($annotation, $text, $update);
            }
            $transport->close();
        }

        $javascript = "";
        $resulthtml = "";
        $socket = new TSocket($host, $port);
        $socket->setRecvTimeout($timeout*1000);
        $transport = new TBufferedTransport($socket, 1024, 1024);
        $transport = new TFramedTransport($transport);
        $protocol = new TBinaryProtocol($transport);
        if ($service == "labeler") {
            $client = new LabelerClient($protocol);
            $transport->open();
            $labeling = $client->labelRecord($record);
            $transport->close();
            $javascript .= getLabelingJavascript($labeling, "labeling");
            $resulthtml .= "<p>";
            $resulthtml .= getLabelingHTML($record->rawText, $labeling, "labeling", true);
            $resulthtml .= "</p>";
            $resulthtml .= "<p>Source (labeling->source): " . $labeling->source . "</p>";
        } else if ($service == "multilabeler") {
            $client = new MultiLabelerClient($protocol);
            $transport->open();
            $labelings = $client->labelRecord($record);
            $transport->close();
            $javascript .= getLabelingJavascript($labeling, "labeling");
            foreach ($labelings as $i => $labeling) {
                $resulthtml .= "<p>Labeling number: ". $i ."</p>";
                $resulthtml .= "<p>";
                $resulthtml .= getLabelingHTML($record->rawText, $labeling, "labeling", true);
            $resulthtml .= "</p>";
                $resulthtml .= "<p>Source (labeling->source): " . $labeling->source . "</p>";
            }
        }else if ($service == "parser") {
            $client = new ParserClient($protocol);
            $transport->open();
            $forest = $client->parseRecord($record);
            $transport->close();
            $javascript .= getForestJavascript($forest, "forest");
            $resulthtml .= "<p>";
            $resulthtml .= getForestHTML($record->rawText, $forest, "forest", true);
            $resulthtml .= "</p>";
            $resulthtml .= "<p>Source (forest->source): " . $forest->source . "</p>";
        } else if ($service == "multiparser") {
            $client = new MultiParserClient($protocol);
            $transport->open();
            $forests = $client->parseRecord($record);
            $transport->close();
            foreach ($forests as $i => $forest) {
                $javascript .= getForestJavascript($forest, "forest");
                $resulthtml .= "<p>Forest number: ". $i ."</p>";
                $resulthtml .= "<p>";
                $resulthtml .= getForestHTML($record->rawText, $forest, "forest", true);
                $resulthtml .= "</p>";
                $resulthtml .= "<p>Source (forest->source): " . $forest->source . "</p>";
            }
        } else {
            $resulthtml .= "<p>YOU DIDN'T SELECT A SERVICE</p>";
        }

        $smarty->assign('text', htmlspecialchars($record->rawText));
        $smarty->assign('jquery', $javascript);
        $smarty->assign('content', $resulthtml);
        $smarty->display('result.tpl');
    } catch (TException $tx) {
        if ($transport->isOpen()) {
            $transport->close();
        }
        echo "<p><code><pre>".$tx->getMessage()."</pre></code></p>";
        echo "<code><pre>".$tx."</pre></code>";


    }
}

    perform();

?>