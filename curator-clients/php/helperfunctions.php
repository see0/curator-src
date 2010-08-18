<?php
$GLOBALS['THRIFT_ROOT'] = 'thrift';
require_once $GLOBALS['THRIFT_ROOT'].'/Thrift.php';
require_once $GLOBALS['THRIFT_ROOT'].'/protocol/TBinaryProtocol.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TSocket.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/THttpClient.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TBufferedTransport.php';
require_once $GLOBALS['THRIFT_ROOT'].'/transport/TFramedTransport.php';
require_once $GLOBALS['THRIFT_ROOT'].'/packages/curator/Curator.php';


function getLabelingHTMLSentences($text, $labeling, $name, $sentences, $newline = false, $offset = 0) {
    if (is_null($labeling)) { return ""; }
    $labels = $labeling->labels;
    $sents = $sentences->labels;
    $result = '<div class="sentence"><p>';
    $j = 0;
    
    foreach ($labels as $i => $span) {
        if ($span->start > $sents[$j]->ending) {
            $result .= '</p></div><div class="sentence"><p>';
            $j = $j + 1;
        }
        $pos = $i+$offset;
        $result .= getSpanHTML($text, $span, $name.$pos);
        if ($newline) $result .= "<br/>";
    }
    $result .= '</p></div>';
    return $result;
}

function getLabelingHTML($text, $labeling, $name, $newline = false, $offset = 0) {
    if (is_null($labeling)) { return ""; }
    $labels = $labeling->labels;
    $result = '';
    foreach ($labels as $i => $span) {
        $pos = $i+$offset;
        $result .= getSpanHTML($text, $span, $name.$pos);
        if ($newline) $result .= "<br/>";
    }
    return $result;
}

function getSpanHTML($text, $span, $name) {
    $result = "";
    $result .= '<span class="span" id="' . $name. '">';
    if (!is_null($span->label)) {
        $result .= '<span class="label '.$span->label.'">';
        $result .= htmlspecialchars($span->label);
        $result .= "</span>";
        $result .= ' ';
    }
    $result .= '<span class="token '.$span->label.'">';
    $result .= htmlspecialchars(substr($text, $span->start, $span->ending - $span->start));
    $result .= '</span>';
    if (!is_null($span->attributes)) {
        foreach ($span->attributes as $key => $value) {
            $result .= ' <span class="attribute ' .$span->label.'">'; 
            $result .= htmlspecialchars("[".$key . ": " . $value."]");
            $result .= "</span>&nbsp;";
        }
    }
    $result .= '</span>&nbsp;';
    return $result;
}

function getListSpanHTML($text, $spans, $name, $newline) {
    if (is_null($spans)) { return ""; }
    $result = '';
    foreach ($spans as $i => $span) {
        $result .= getSpanHTML($text, $span, $name.$i);
        if ($newline) $result .= "<br/>";
    }
    return $result;
}

function getLabelingJavascript($labeling, $name, $offset=0) {
    if (is_null($labeling)) { return ""; }
    $labels = $labeling->labels;
    $result = "";
    foreach ($labels as $i => $span) {
        $result .= getSpanJavascript($span, $name, $i+$offset);
    }
    return $result;
}

function getListSpanJavascript($spans, $name) {
    if (is_null($spans)) { return ""; }
    $result = "";
    foreach ($spans as $i => $span) {
        $result .= getSpanJavascript($span, $name, $i);
    }
    return $result;
}

function getSpanJavascript($span, $name, $i) {
    if (is_null($span)) { return  ""; }
    $result = '$("#' . $name . $i . '").click( function() {'. "\n";
    $result .= '$("#text").removeHighlight();' . "\n";
    $result .= '$("#text").highlight(' . "$span->start, $span->ending, 'highlight');\n";
    $result .= "});\n";
    return $result;
}

function getSrlJavascript($forest, $name) {
    if (is_null($forest)) { return ""; }
    $result = '';
    $counter = 0;
    foreach ($forest->trees as $i => $tree) {
        $nodes = $tree->nodes;
        $top = $nodes[$tree->top];
        $result .= getSpanJavascript($top->span, $name, $counter);
        $counter++;
        foreach ($top->children as $child => $label) {
            $result .= getSpanJavascript($nodes[$child]->span, $name, $counter);
            $counter++;
        }
    }
    return $result;
}

function getSrlHTML($text, $forest, $name) {
    if (is_null($forest)) { return ""; }
    $result = '';
    $counter = 0;
    foreach ($forest->trees as $i => $tree) {
        $nodes = $tree->nodes;
        $top = $nodes[$tree->top];
        $result .= '<span id="'. $name.$counter.'">(';
        $result .= $top->span->attribute ." : " .substr($text, $top->span->start, $top->span->ending - $top->span->start);
        $result .= '</span><br/>';
        $counter++;
        foreach ($top->children as $child => $label) {
            $result .= '&nbsp;&nbsp;';
            $result .= '<span id="' .$name.$counter.'">(<span class="edge">'. $label  . '</span> : ';
            $result .= substr($text, $nodes[$child]->span->start, $nodes[$child]->span->ending - $nodes[$child]->span->start);
            $result .= ')</span><br/>';
            $counter++;
        }
        $result .= ')<br/><br/>';
    }
    return $result;
}

function getForestHTML($text, $forest, $name, $spans=false) {
    if (is_null($forest)) { return ""; }
    $result = "";
    $counter = 0;

    foreach ($forest->trees as $i => $tree) {
        $topnode = $tree->nodes[$tree->top];
        if (isset($tree->score)) {
            $result .= 'Score: ' .$tree->score .'<br/>';
        }
        $result .= getNodeHTML($text, $topnode, $tree->nodes, 0, $name, $counter, "", true, $spans);
        $result .= "<br/>";
    }
    return $result;
}

function getForestJavascript($forest, $name) {
    if (is_null($forest)) { return ""; }
    $counter = 0;
    $result = "";
    foreach ($forest->trees as $i => $tree) {
        $topnode = $tree->nodes[$tree->top];
        $result .= getNodeJavascript($topnode, $tree->nodes, $name, $counter);
    }
    return $result;

}

function getNodeHTML($text, $node, $nodes, $padding, $name, &$counter, $label="", $first=true, $spans=false) {
    $result = "";
    $dpadding = 0;
    $counter += 1;
    if (!$first) {
        for ($i = 0; $i < $padding; $i++) {
            $result .= "&nbsp;";
        }
    }
    if (!($label == "")) {
        $result .= "<span class='edge'>&lt;$label&gt;</span>&nbsp;";
        $dpadding += strlen($label) + 2;
    }
    $span = $node->span;
    if (!($node->label == "dependency node")) {
        $result .= '<span class="label" id="' .$name . $counter. '">'.$node->label.'</span>&nbsp;&nbsp;';
        $dpadding += strlen($node->label) + 2;
    }

    if ($spans) {
        $result .= getSpanHTML($text, $span, $name.$counter);
        $result .= '<br/>';
        //$dpadding += $span->ending-$span->start+1;
        $dpadding++;
    }
    if (!is_null($node->children) && !empty($node->children)) {
        ksort($node->children);
        $padding += $dpadding;
        $first = true;
        if ($spans) {
            $first = false;
        }
        foreach ($node->children as $index => $nlabel) {
            $result .= getNodeHTML($text, $nodes[$index], $nodes, $padding, $name, $counter, $nlabel, $first, $spans);
            $first = false;
            
        }
    } else if (!$spans) {
        $result .= getSpanHTML($text, $span, "");
        $result .= '<br/>';
    }
    return $result;
}

function getNodeJavascript($node, $nodes, $name, &$counter) {
    $counter++;
    $result = "";
    $result .= getSpanJavascript($node->span, $name, $counter);
    if (!is_null($node->children) && !empty($node->children)) {
        ksort($node->children);
        foreach ($node->children as $index => $nlabel) {
            $result .= getNodeJavascript($nodes[$index], $nodes, $name, $counter);
        }
    }
    return $result;
}

?>