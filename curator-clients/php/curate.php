<?php
ini_set('error_reporting', E_ALL);
ini_set('display_errors',1);
$GLOBALS['THRIFT_ROOT'] = 'thrift';
require_once $GLOBALS['THRIFT_ROOT'].'/Thrift.php';
require_once $GLOBALS['THRIFT_ROOT'].'/protocol/TBinaryProtocol.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TSocket.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/THttpClient.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TBufferedTransport.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TFramedTransport.php';

require_once $GLOBALS['THRIFT_ROOT'].'/packages/curator/Curator.php';

require_once 'helperfunctions.php';
require_once 'CURATORCONFIG.php';

define('SMARTY_DIR', 'smarty/');

require_once SMARTY_DIR.'Smarty.class.php';

function sanitize_text($text) {
    return str_replace(array("\r\n", "\r", "\n"), "\n", stripslashes($text));
}

function perform() {
    $smarty = new Smarty;

    $text = sanitize_text($_POST["text"]);
    $trtext = trim($text);
    if (empty($trtext)) {
        header('Location: ./');
    }
    $update = isset($_POST['force']) && $_POST['force'] == 'true';
    $ws = isset($_POST['ws']) && $_POST['ws'] == 'true';
    $annotations = $_POST['annotations'];

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
        $content = "<p><span class='info'>This record has identifier:</span> ".$record->identifier . "</p>";
        if ($record->whitespaced) {
            $content .= "<p class='info'>This record was based on whitespace tokenization.</p>";
        }
        
        foreach ($record->labelViews as $view_name => $labeling) {
            $javascript .= getLabelingJavascript($labeling, $view_name);
            $content .= "<h2>" .$view_name. " view</h2>";
            $content .= "<p>";
            $content .= getLabelingHTML($record->rawText, $labeling, $view_name, true);
            $content .= "</p>";
            $content .= "<p>source: " .$labeling->source . "</p>";
        }
        foreach ($record->clusterViews as $view_name => $clustering) {
            $offset = 0;
            $content .= "<h2>".$view_name." view</h2>";
            $content .= "<p>";
            foreach ($clustering->clusters as $i => $labeling) {
                $content .= "<div style='border: 1px solid; float: left; padding-right:15px; margin-bottom:5px;'><strong>Cluster $i</strong>&nbsp;&nbsp;&nbsp;<br/>";
                $content .= getLabelingHTML($record->rawText, $labeling, $view_name, true, $offset);
                $content .= "</div>";
                $javascript .= getLabelingJavascript($labeling, $view_name, $offset);
                $offset += count($labeling->labels);
            }
            $content .= "</p>";
            $content .= "<p>source: " .$clustering->source . "</p>";
        }
        foreach ($record->parseViews as $view_name => $forest) {
            $javascript .= getForestJavascript($forest, $view_name);
            $content .= "<h2>" .$view_name. " View</h2>";
            $content .= "<p>";
            $content .= getForestHTML($record->rawText, $forest, $view_name, true);
            $content .= "</p>";
            $content .= "<p>source: " .$forest->source . "</p>";
        }



        $smarty->assign('text', htmlspecialchars($record->rawText));
        $smarty->assign('jquery', $javascript);
        $smarty->assign('content', $content);
        $smarty->display('result.tpl');
    } catch (base_AnnotationFailedException $af) {
        if ($transport->isOpen()) {
            $transport->close();
        }
        echo "<p><code>Annotation failed: $af->reason</p>";
        echo "<code><pre>";
        echo $af;
        echo "</pre></code>";
    } catch (TException $tx) {
        if ($transport->isOpen()) {
            $transport->close();
        }
        echo "<p><code><pre>".$tx->getMessage()."</pre></code></p>";
        echo "<code><pre>";
        echo $tx;
        echo "</pre></code>";
    }
}
    perform();

?>